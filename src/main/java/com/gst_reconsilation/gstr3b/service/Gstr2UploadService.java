package com.gst_reconsilation.gstr3b.service;

import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.gstr3b.dto.Gstr2UploadResponse;
import com.gst_reconsilation.gstr3b.entity.*;
import com.gst_reconsilation.gstr3b.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class Gstr2UploadService {

    private final Gstr2FilingRepository filingRepository;
    private final CompanyGSTRepository  companyGSTRepository;
    private final Gstr2ExcelParserService excelParserService;

    private final Gstr2B2bRepository    b2bRepository;
    private final Gstr2B2burRepository  b2burRepository;
    private final Gstr2ImpsRepository   impsRepository;
    private final Gstr2ImpgRepository   impgRepository;
    private final Gstr2CdnrRepository   cdnrRepository;
    private final Gstr2CdnurRepository  cdnurRepository;
    private final Gstr2AtRepository     atRepository;
    private final Gstr2AtadjRepository  atadjRepository;
    private final Gstr2ExempRepository  exempRepository;
    private final Gstr2ItcrRepository   itcrRepository;
    private final Gstr2HsnSumRepository hsnSumRepository;

    @Value("${app.excel.upload-dir}")
    private String uploadDir;

    @Transactional
    public Gstr2UploadResponse uploadAndProcess(
            MultipartFile file,
            Integer companyGstId,
            String financialYear,
            String taxPeriod,
            Integer userId) throws Exception {

        // 1. Validate file type
        String originalName = file.getOriginalFilename();
        if (originalName == null ||
                (!originalName.endsWith(".xlsx") && !originalName.endsWith(".xls"))) {
            throw new IllegalArgumentException("Only .xlsx or .xls files are accepted");
        }

        // 2. Resolve CompanyGST
        CompanyGST companyGST = companyGSTRepository.findById(companyGstId)
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + companyGstId));

        // 3. Save file to disk
        String savedPath = saveFileToDisk(file, companyGST.getGstNumber(), financialYear, taxPeriod);

        // 4. Create or update filing record
        Gstr2Filing filing = filingRepository
                .findByCompanyGST_IdAndFinancialYearAndTaxPeriod(companyGstId, financialYear, taxPeriod)
                .orElseGet(() -> Gstr2Filing.builder()
                        .companyGST(companyGST)
                        .financialYear(financialYear)
                        .taxPeriod(taxPeriod)
                        .createdBy(userId)
                        .build());

        filing.setExcelFilePath(savedPath);
        filing.setOriginalFileName(originalName);
        filing.setFilingStatus("DRAFT");
        filing.setUpdatedBy(userId);
        filing.setUpdatedDate(LocalDate.now());
        filing = filingRepository.save(filing);

        // 5. Delete previously imported data for this filing (re-upload scenario)
        deleteAllByFilingId(filing.getId());

        // 6. Parse all sheets
        Gstr2ExcelParserService.ParseResult parsed;
        try (var is = Files.newInputStream(Paths.get(savedPath))) {
            parsed = excelParserService.parse(is, filing);
        }

        // 7. Persist all sheet data
        int b2b    = b2bRepository.saveAll(parsed.b2b).size();
        int b2bur  = b2burRepository.saveAll(parsed.b2bur).size();
        int imps   = impsRepository.saveAll(parsed.imps).size();
        int impg   = impgRepository.saveAll(parsed.impg).size();
        int cdnr   = cdnrRepository.saveAll(parsed.cdnr).size();
        int cdnur  = cdnurRepository.saveAll(parsed.cdnur).size();
        int at     = atRepository.saveAll(parsed.at).size();
        int atadj  = atadjRepository.saveAll(parsed.atadj).size();
        int exemp  = exempRepository.saveAll(parsed.exemp).size();
        int itcr   = itcrRepository.saveAll(parsed.itcr).size();
        int hsnSum = hsnSumRepository.saveAll(parsed.hsnSum).size();

        int total = b2b + b2bur + imps + impg + cdnr + cdnur + at + atadj + exemp + itcr + hsnSum;

        log.info("Filing {}: imported {} total rows from '{}'", filing.getId(), total, originalName);

        // 8. Build response
        return Gstr2UploadResponse.builder()
                .filingId(filing.getId())
                .financialYear(financialYear)
                .taxPeriod(taxPeriod)
                .filingStatus(filing.getFilingStatus())
                .excelFilePath(savedPath)
                .totalRowsImported(total)
                .b2bRows(b2b).b2burRows(b2bur).impsRows(imps).impgRows(impg)
                .cdnrRows(cdnr).cdnurRows(cdnur).atRows(at).atadjRows(atadj)
                .exempRows(exemp).itcrRows(itcr).hsnSumRows(hsnSum)
                .build();
    }

    // ── Queries ───────────────────────────────────────────────────

    public List<Gstr2Filing> getFilingsByCompanyGST(Integer companyGstId) {
        return filingRepository.findByCompanyGST_IdAndIsActiveTrue(companyGstId);
    }

    public Gstr2Filing getFilingById(Integer filingId) {
        return filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Filing not found: " + filingId));
    }

    public List<Gstr2B2b>    getB2b(Integer filingId)    { return b2bRepository.findByFiling_Id(filingId); }
    public List<Gstr2B2bur>  getB2bur(Integer filingId)  { return b2burRepository.findByFiling_Id(filingId); }
    public List<Gstr2Imps>   getImps(Integer filingId)   { return impsRepository.findByFiling_Id(filingId); }
    public List<Gstr2Impg>   getImpg(Integer filingId)   { return impgRepository.findByFiling_Id(filingId); }
    public List<Gstr2Cdnr>   getCdnr(Integer filingId)   { return cdnrRepository.findByFiling_Id(filingId); }
    public List<Gstr2Cdnur>  getCdnur(Integer filingId)  { return cdnurRepository.findByFiling_Id(filingId); }
    public List<Gstr2At>     getAt(Integer filingId)     { return atRepository.findByFiling_Id(filingId); }
    public List<Gstr2Atadj>  getAtadj(Integer filingId)  { return atadjRepository.findByFiling_Id(filingId); }
    public List<Gstr2Exemp>  getExemp(Integer filingId)  { return exempRepository.findByFiling_Id(filingId); }
    public List<Gstr2Itcr>   getItcr(Integer filingId)   { return itcrRepository.findByFiling_Id(filingId); }
    public List<Gstr2HsnSum> getHsnSum(Integer filingId) { return hsnSumRepository.findByFiling_Id(filingId); }

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Deletes all sheet rows for a filing — used before re-importing on re-upload.
     */
    private void deleteAllByFilingId(Integer filingId) {
        b2bRepository.deleteByFiling_Id(filingId);
        b2burRepository.deleteByFiling_Id(filingId);
        impsRepository.deleteByFiling_Id(filingId);
        impgRepository.deleteByFiling_Id(filingId);
        cdnrRepository.deleteByFiling_Id(filingId);
        cdnurRepository.deleteByFiling_Id(filingId);
        atRepository.deleteByFiling_Id(filingId);
        atadjRepository.deleteByFiling_Id(filingId);
        exempRepository.deleteByFiling_Id(filingId);
        itcrRepository.deleteByFiling_Id(filingId);
        hsnSumRepository.deleteByFiling_Id(filingId);
    }

    private String saveFileToDisk(MultipartFile file, String gstNumber,
                                  String financialYear, String taxPeriod) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uid       = UUID.randomUUID().toString().substring(0, 8);
        String safeName  = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName  = timestamp + "_" + uid + "_" + safeName;

        Path dir = Paths.get(uploadDir, gstNumber, financialYear, taxPeriod, "gstr2");
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        file.transferTo(target.toFile());

        log.info("GSTR-2 Excel saved: {}", target.toAbsolutePath());
        return target.toAbsolutePath().toString();
    }
}