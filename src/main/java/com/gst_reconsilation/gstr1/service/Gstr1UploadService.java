package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.gstr1.dto.Gstr1UploadResponse;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.gstr1.entity.*;
import com.gst_reconsilation.gstr1.entity.Gstr1Cdnura;
import com.gst_reconsilation.gstr1.repository.*;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
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
public class Gstr1UploadService {

    private final Gstr1FilingRepository     filingRepository;
    private final CompanyGSTRepository      companyGSTRepository;
    private final ExcelParserService        excelParserService;

    // All sheet repositories
    private final Gstr1B2bRepository       b2bRepository;
    private final Gstr1B2baRepository      b2baRepository;
    private final Gstr1B2clRepository      b2clRepository;
    private final Gstr1B2claRepository     b2claRepository;
    private final Gstr1B2csRepository      b2csRepository;
    private final Gstr1B2csaRepository     b2csaRepository;
    private final Gstr1CdnrRepository      cdnrRepository;
    private final Gstr1CdnraRepository     cdnraRepository;
    private final Gstr1CdnurRepository     cdnurRepository;
    private final Gstr1CdnuraRepository    cdnuraRepository;
    private final Gstr1ExpRepository       expRepository;
    private final Gstr1ExpaRepository      expaRepository;
    private final Gstr1AtRepository        atRepository;
    private final Gstr1AtaRepository       ataRepository;
    private final Gstr1AtadjRepository     atadjRepository;
    private final Gstr1AtadjaRepository    atadjаRepository;
    private final Gstr1ExempRepository     exempRepository;
    private final Gstr1HsnB2bRepository    hsnB2bRepository;
    private final Gstr1HsnB2cRepository    hsnB2cRepository;
    private final Gstr1DocsRepository      docsRepository;
    private final Gstr1EcoRepository       ecoRepository;
    private final Gstr1EcoaRepository      ecoaRepository;
    private final Gstr1EcoB2bRepository    ecoB2bRepository;
    private final Gstr1EcoUrp2bRepository  ecoUrp2bRepository;
    private final Gstr1EcoB2cRepository    ecoB2cRepository;
    private final Gstr1EcoUrp2cRepository  ecoUrp2cRepository;
    private final Gstr1EcoAB2bRepository   ecoAB2bRepository;
    private final Gstr1EcoAB2cRepository   ecoAB2cRepository;
    private final Gstr1EcoAUrp2bRepository ecoAUrp2bRepository;
    private final Gstr1EcoAUrp2cRepository ecoAUrp2cRepository;

    @Value("${app.excel.upload-dir}")
    private String uploadDir;

    @Transactional
    public Gstr1UploadResponse uploadAndProcess(
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
        Gstr1Filing filing = filingRepository
                .findByCompanyGST_IdAndFinancialYearAndTaxPeriod(companyGstId, financialYear, taxPeriod)
                .orElseGet(() -> Gstr1Filing.builder()
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
        ExcelParserService.ParseResult parsed;
        try (var is = Files.newInputStream(Paths.get(savedPath))) {
            parsed = excelParserService.parse(is, filing);
        }

        // 7. Persist all sheet data
        int b2b       = b2bRepository.saveAll(parsed.b2b).size();
        int b2ba      = b2baRepository.saveAll(parsed.b2ba).size();
        int b2cl      = b2clRepository.saveAll(parsed.b2cl).size();
        int b2cla     = b2claRepository.saveAll(parsed.b2cla).size();
        int b2cs      = b2csRepository.saveAll(parsed.b2cs).size();
        int b2csa     = b2csaRepository.saveAll(parsed.b2csa).size();
        int cdnr      = cdnrRepository.saveAll(parsed.cdnr).size();
        int cdnra     = cdnraRepository.saveAll(parsed.cdnra).size();
        int cdnur     = cdnurRepository.saveAll(parsed.cdnur).size();
        int cdnura    = cdnuraRepository.saveAll(parsed.cdnura).size();
        int exp       = expRepository.saveAll(parsed.exp).size();
        int expa      = expaRepository.saveAll(parsed.expa).size();
        int at        = atRepository.saveAll(parsed.at).size();
        int ata       = ataRepository.saveAll(parsed.ata).size();
        int atadj     = atadjRepository.saveAll(parsed.atadj).size();
        int atadja    = atadjаRepository.saveAll(parsed.atadja).size();
        int exemp     = exempRepository.saveAll(parsed.exemp).size();
        int hsnB2b    = hsnB2bRepository.saveAll(parsed.hsnB2b).size();
        int hsnB2c    = hsnB2cRepository.saveAll(parsed.hsnB2c).size();
        int docs      = docsRepository.saveAll(parsed.docs).size();
        int eco       = ecoRepository.saveAll(parsed.eco).size();
        int ecoa      = ecoaRepository.saveAll(parsed.ecoa).size();
        int ecoB2b    = ecoB2bRepository.saveAll(parsed.ecoB2b).size();
        int ecoUrp2b  = ecoUrp2bRepository.saveAll(parsed.ecoUrp2b).size();
        int ecoB2c    = ecoB2cRepository.saveAll(parsed.ecoB2c).size();
        int ecoUrp2c  = ecoUrp2cRepository.saveAll(parsed.ecoUrp2c).size();
        int ecoAB2b   = ecoAB2bRepository.saveAll(parsed.ecoAB2b).size();
        int ecoAB2c   = ecoAB2cRepository.saveAll(parsed.ecoAB2c).size();
        int ecoAUrp2b = ecoAUrp2bRepository.saveAll(parsed.ecoAUrp2b).size();
        int ecoAUrp2c = ecoAUrp2cRepository.saveAll(parsed.ecoAUrp2c).size();

        int total = b2b+b2ba+b2cl+b2cla+b2cs+b2csa+cdnr+cdnra+cdnur+cdnura
                +exp+expa+at+ata+atadj+atadja+exemp+hsnB2b+hsnB2c+docs
                +eco+ecoa+ecoB2b+ecoUrp2b+ecoB2c+ecoUrp2c+ecoAB2b+ecoAB2c+ecoAUrp2b+ecoAUrp2c;

        log.info("Filing {}: imported {} total rows from '{}'", filing.getId(), total, originalName);

        // 8. Build response
        return Gstr1UploadResponse.builder()
                .filingId(filing.getId())
                .financialYear(financialYear)
                .taxPeriod(taxPeriod)
                .filingStatus(filing.getFilingStatus())
                .excelFilePath(savedPath)
                .totalRowsImported(total)
                .b2bRows(b2b).b2baRows(b2ba).b2clRows(b2cl).b2claRows(b2cla)
                .b2csRows(b2cs).b2csaRows(b2csa).cdnrRows(cdnr).cdnraRows(cdnra)
                .cdnurRows(cdnur).cdnuraRows(cdnura).expRows(exp).expaRows(expa)
                .atRows(at).ataRows(ata).atadjRows(atadj).atadjаRows(atadja)
                .exempRows(exemp).hsnB2bRows(hsnB2b).hsnB2cRows(hsnB2c).docsRows(docs)
                .ecoRows(eco).ecoaRows(ecoa).ecoB2bRows(ecoB2b).ecoUrp2bRows(ecoUrp2b)
                .ecoB2cRows(ecoB2c).ecoUrp2cRows(ecoUrp2c).ecoAB2bRows(ecoAB2b)
                .ecoAB2cRows(ecoAB2c).ecoAUrp2bRows(ecoAUrp2b).ecoAUrp2cRows(ecoAUrp2c)
                .build();
    }

    // ── Queries ───────────────────────────────────────────────────

    public List<Gstr1Filing> getFilingsByCompanyGST(Integer companyGstId) {
        return filingRepository.findByCompanyGST_IdAndIsActiveTrue(companyGstId);
    }

    public Gstr1Filing getFilingById(Integer filingId) {
        return filingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("Filing not found: " + filingId));
    }

    public List<Gstr1B2b>    getB2b(Integer filingId)    { return b2bRepository.findByFiling_Id(filingId); }
    public List<Gstr1B2ba>   getB2ba(Integer filingId)   { return b2baRepository.findByFiling_Id(filingId); }
    public List<Gstr1B2cl>   getB2cl(Integer filingId)   { return b2clRepository.findByFiling_Id(filingId); }
    public List<Gstr1B2cla>  getB2cla(Integer filingId)  { return b2claRepository.findByFiling_Id(filingId); }
    public List<Gstr1B2cs>   getB2cs(Integer filingId)   { return b2csRepository.findByFiling_Id(filingId); }
    public List<Gstr1B2csa>  getB2csa(Integer filingId)  { return b2csaRepository.findByFiling_Id(filingId); }
    public List<Gstr1Cdnr>   getCdnr(Integer filingId)   { return cdnrRepository.findByFiling_Id(filingId); }
    public List<Gstr1Cdnra>  getCdnra(Integer filingId)  { return cdnraRepository.findByFiling_Id(filingId); }
    public List<Gstr1Cdnur>  getCdnur(Integer filingId)  { return cdnurRepository.findByFiling_Id(filingId); }
    public List<Gstr1Cdnura> getCdnura(Integer filingId) { return cdnuraRepository.findByFiling_Id(filingId); }
    public List<Gstr1Exp>    getExp(Integer filingId)    { return expRepository.findByFiling_Id(filingId); }
    public List<Gstr1Expa>   getExpa(Integer filingId)   { return expaRepository.findByFiling_Id(filingId); }
    public List<Gstr1At>     getAt(Integer filingId)     { return atRepository.findByFiling_Id(filingId); }
    public List<Gstr1Ata>    getAta(Integer filingId)    { return ataRepository.findByFiling_Id(filingId); }
    public List<Gstr1Atadj>  getAtadj(Integer filingId)  { return atadjRepository.findByFiling_Id(filingId); }
    public List<Gstr1Atadja> getAtadja(Integer filingId) { return atadjаRepository.findByFiling_Id(filingId); }
    public List<Gstr1Exemp>  getExemp(Integer filingId)  { return exempRepository.findByFiling_Id(filingId); }
    public List<Gstr1HsnB2b> getHsnB2b(Integer filingId) { return hsnB2bRepository.findByFiling_Id(filingId); }
    public List<Gstr1HsnB2c> getHsnB2c(Integer filingId) { return hsnB2cRepository.findByFiling_Id(filingId); }
    public List<Gstr1Docs>   getDocs(Integer filingId)   { return docsRepository.findByFiling_Id(filingId); }
    public List<Gstr1Eco>    getEco(Integer filingId)    { return ecoRepository.findByFiling_Id(filingId); }
    public List<Gstr1Ecoa>   getEcoa(Integer filingId)   { return ecoaRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoB2b>    getEcoB2b(Integer filingId)    { return ecoB2bRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoUrp2b>  getEcoUrp2b(Integer filingId)  { return ecoUrp2bRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoB2c>    getEcoB2c(Integer filingId)    { return ecoB2cRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoUrp2c>  getEcoUrp2c(Integer filingId)  { return ecoUrp2cRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoAB2b>   getEcoAB2b(Integer filingId)   { return ecoAB2bRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoAB2c>   getEcoAB2c(Integer filingId)   { return ecoAB2cRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoAUrp2b> getEcoAUrp2b(Integer filingId) { return ecoAUrp2bRepository.findByFiling_Id(filingId); }
    public List<Gstr1EcoAUrp2c> getEcoAUrp2c(Integer filingId) { return ecoAUrp2cRepository.findByFiling_Id(filingId); }

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Deletes all sheet rows for a filing — used before re-importing on re-upload.
     */
    private void deleteAllByFilingId(Integer filingId) {
        b2bRepository.deleteByFiling_Id(filingId);
        b2baRepository.deleteByFiling_Id(filingId);
        b2clRepository.deleteByFiling_Id(filingId);
        b2claRepository.deleteByFiling_Id(filingId);
        b2csRepository.deleteByFiling_Id(filingId);
        b2csaRepository.deleteByFiling_Id(filingId);
        cdnrRepository.deleteByFiling_Id(filingId);
        cdnraRepository.deleteByFiling_Id(filingId);
        cdnurRepository.deleteByFiling_Id(filingId);
        cdnuraRepository.deleteByFiling_Id(filingId);
        expRepository.deleteByFiling_Id(filingId);
        expaRepository.deleteByFiling_Id(filingId);
        atRepository.deleteByFiling_Id(filingId);
        ataRepository.deleteByFiling_Id(filingId);
        atadjRepository.deleteByFiling_Id(filingId);
        atadjаRepository.deleteByFiling_Id(filingId);
        exempRepository.deleteByFiling_Id(filingId);
        hsnB2bRepository.deleteByFiling_Id(filingId);
        hsnB2cRepository.deleteByFiling_Id(filingId);
        docsRepository.deleteByFiling_Id(filingId);
        ecoRepository.deleteByFiling_Id(filingId);
        ecoaRepository.deleteByFiling_Id(filingId);
        ecoB2bRepository.deleteByFiling_Id(filingId);
        ecoUrp2bRepository.deleteByFiling_Id(filingId);
        ecoB2cRepository.deleteByFiling_Id(filingId);
        ecoUrp2cRepository.deleteByFiling_Id(filingId);
        ecoAB2bRepository.deleteByFiling_Id(filingId);
        ecoAB2cRepository.deleteByFiling_Id(filingId);
        ecoAUrp2bRepository.deleteByFiling_Id(filingId);
        ecoAUrp2cRepository.deleteByFiling_Id(filingId);
    }

    private String saveFileToDisk(MultipartFile file, String gstNumber,
                                  String financialYear, String taxPeriod) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uid       = UUID.randomUUID().toString().substring(0, 8);
        String safeName  = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName  = timestamp + "_" + uid + "_" + safeName;

        Path dir = Paths.get(uploadDir, gstNumber, financialYear, taxPeriod);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        file.transferTo(target.toFile());

        log.info("Excel saved: {}", target.toAbsolutePath());
        return target.toAbsolutePath().toString();
    }
}