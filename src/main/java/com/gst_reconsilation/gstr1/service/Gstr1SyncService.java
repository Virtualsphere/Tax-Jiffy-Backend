package com.gst_reconsilation.gstr1.service;

import com.gst_reconsilation.apiusage.exception.ApiUsageLimitExceededException;
import com.gst_reconsilation.apiusage.service.ApiUsageService;
import com.gst_reconsilation.company.entity.CompanyGST;
import com.gst_reconsilation.company.repository.CompanyGSTRepository;
import com.gst_reconsilation.config.GstApiCredentialsProperties;
import com.gst_reconsilation.gstr1.entity.*;
import com.gst_reconsilation.gstr1.repository.*;
import com.gst_reconsilation.gstr1.dto.SyncRequest;
import com.gst_reconsilation.gstr1.dto.SyncResponse;
import com.gst_reconsilation.gstr1.dto.api.*;
import com.gst_reconsilation.whitebooks.GstPeriods;
import com.gst_reconsilation.whitebooks.WhitebooksApiException;
import com.gst_reconsilation.whitebooks.WhitebooksClient;
import com.gst_reconsilation.whitebooks.session.GstAuthService;
import com.gst_reconsilation.whitebooks.session.GstSessionRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class Gstr1SyncService {

    private final CompanyGSTRepository      companyGSTRepository;
    private final Gstr1FilingRepository     filingRepository;
    private final Gstr1B2bRepository        b2bRepository;
    private final Gstr1B2csRepository       b2csRepository;
    private final Gstr1B2csaRepository      b2csaRepository;
    private final Gstr1B2claRepository      b2claRepository;
    private final Gstr1CdnrRepository       cdnrRepository;
    private final Gstr1CdnurRepository      cdnurRepository;
    private final Gstr1CdnuraRepository     cdnuraRepository;
    private final Gstr1ExpRepository        expRepository;
    private final Gstr1ExpaRepository       expaRepository;
    private final Gstr1AtRepository         atRepository;
    private final Gstr1AtaRepository        ataRepository;
    private final Gstr1ExempRepository      exempRepository;
    private final Gstr1HsnB2bRepository     hsnB2bRepository;
    private final Gstr1HsnB2cRepository     hsnB2cRepository;
    private final WhitebooksClient          client;
    private final GstAuthService            gstAuthService;
    private final GstApiCredentialsProperties props;
    private final ApiUsageService           apiUsageService;

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ─────────────────────────────────────────────────────────────
    //  MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public SyncResponse sync(SyncRequest req, Integer userId) {
        CompanyGST companyGST = companyGSTRepository.findById(req.getCompanyGstId())
                .orElseThrow(() -> new RuntimeException("CompanyGST not found: " + req.getCompanyGstId()));

        // Fails fast with HTTP 428 before touching any data if there is no verified GST session
        HttpHeaders headers = gstAuthService.headersFor(companyGST.getId());
        // HTTP 429 up front if the monthly API budget can't cover all 13 section calls
        apiUsageService.ensureRemaining(companyGST.getId(), SECTION_COUNT, "GSTR-1 sync");
        SyncCtx ctx = new SyncCtx(companyGST.getId(), companyGST.getGstNumber(),
                GstPeriods.toRetPeriod(req.getFinancialYear(), req.getTaxPeriod()), headers);

        // Get or create filing record
        Gstr1Filing filing = filingRepository
                .findByCompanyGST_IdAndFinancialYearAndTaxPeriod(
                        req.getCompanyGstId(), req.getFinancialYear(), req.getTaxPeriod())
                .orElseGet(() -> Gstr1Filing.builder()
                        .companyGST(companyGST)
                        .financialYear(req.getFinancialYear())
                        .taxPeriod(req.getTaxPeriod())
                        .createdBy(userId)
                        .build());

        filing.setFilingStatus("SYNCING");
        filing.setUpdatedBy(userId);
        filing.setUpdatedDate(LocalDate.now());
        filing = filingRepository.save(filing);

        // Delete existing rows before re-syncing
        clearExistingRows(filing.getId());

        int b2b = 0, b2cs = 0, b2csa = 0, b2cla = 0;
        int cdnr = 0, cdnur = 0, cdnura = 0;
        int exp = 0, expa = 0, at = 0, ata = 0;
        int exemp = 0, hsnB2b = 0, hsnB2c = 0;

        // Fetch and persist each sheet
        b2b   = syncB2b(filing, ctx);
        b2cs  = syncB2cs(filing, ctx);
        b2csa = syncB2csa(filing, ctx);
        b2cla = syncB2cla(filing, ctx);
        cdnr  = syncCdnr(filing, ctx);
        cdnur = syncCdnur(filing, ctx);
        cdnura = syncCdnura(filing, ctx);
        exp   = syncExp(filing, ctx);
        expa  = syncExpa(filing, ctx);
        at    = syncAt(filing, ctx);
        ata   = syncTxp(filing, ctx);   // TXP → Gstr1Ata
        exemp = syncNil(filing, ctx);
        int[] hsn = syncHsn(filing, ctx);
        hsnB2b = hsn[0];
        hsnB2c = hsn[1];

        int total = b2b+b2cs+b2csa+b2cla+cdnr+cdnur+cdnura
                +exp+expa+at+ata+exemp+hsnB2b+hsnB2c;

        if (ctx.errors.size() >= SECTION_COUNT) {
            // Every section failed: throwing rolls back the row deletion above, so previous data is kept
            throw new WhitebooksApiException("GSTR1_SYNC_FAILED",
                    "GSTR-1 sync failed for every section. First error: " + ctx.errors.get(0));
        }

        filing.setFilingStatus("SYNCED");
        filing.setUpdatedDate(LocalDate.now());
        filingRepository.save(filing);

        log.info("Sync complete for filing {}: {} total rows, {} section errors", filing.getId(), total, ctx.errors.size());

        return SyncResponse.builder()
                .filingId(filing.getId())
                .financialYear(req.getFinancialYear())
                .taxPeriod(req.getTaxPeriod())
                .status("SYNCED")
                .errors(ctx.errors)
                .totalRowsSynced(total)
                .b2bRows(b2b).b2csRows(b2cs).b2csaRows(b2csa).b2claRows(b2cla)
                .cdnrRows(cdnr).cdnurRows(cdnur).cdnuraRows(cdnura)
                .expRows(exp).expaRows(expa).atRows(at).ataRows(ata)
                .exempRows(exemp).hsnB2bRows(hsnB2b).hsnB2cRows(hsnB2c)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    //  PER-SHEET SYNC METHODS
    // ─────────────────────────────────────────────────────────────

    private int syncB2b(Gstr1Filing filing, SyncCtx ctx) {
        try {
            B2bApiResponse resp = get("/b2b", ctx, B2bApiResponse.class);
            if (resp == null || resp.getB2b() == null) return 0;

            List<Gstr1B2b> rows = new ArrayList<>();
            for (var entry : resp.getB2b()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    if (inv.getItms() == null) continue;
                    for (var item : inv.getItms()) {
                        ItemDetail det = item.getDetail();
                        rows.add(Gstr1B2b.builder()
                                .filing(filing)
                                .createdBy(filing.getCreatedBy())
                                .gstinOfRecipient(upper(entry.getCtin()))
                                .invoiceNumber(inv.getInvoiceNumber())
                                .invoiceDate(parseDate(inv.getInvoiceDate()))
                                .invoiceValue(inv.getInvoiceValue())
                                .placeOfSupply(inv.getPos())
                                .reverseCharge(inv.getReverseCharge())
                                .invoiceType(inv.getInvoiceType())
                                .ecommerceGstin(inv.getEtin())
                                .rate(det != null ? det.getRate() : null)
                                .taxableValue(z(det != null ? det.getTaxableValue() : null))
                                .cessAmount(z(det != null ? det.getCsamt() : null))
                                .build());
                    }
                }
            }
            return b2bRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("B2B", e);
            return 0;
        }
    }

    private int syncB2cs(Gstr1Filing filing, SyncCtx ctx) {
        try {
            B2csApiResponse resp = get("/b2cs", ctx, B2csApiResponse.class);
            if (resp == null || resp.getB2cs() == null) return 0;

            List<Gstr1B2cs> rows = new ArrayList<>();
            for (var entry : resp.getB2cs()) {
                rows.add(Gstr1B2cs.builder()
                        .filing(filing)
                        .createdBy(filing.getCreatedBy())
                        .type(entry.getType())
                        .placeOfSupply(entry.getPos())
                        .rate(entry.getRate())
                        .taxableValue(z(entry.getTxval()))
                        .cessAmount(z(entry.getCsamt()))
                        .ecommerceGstin(entry.getEtin())
                        .build());
            }
            return b2csRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("B2CS", e);
            return 0;
        }
    }

    private int syncB2csa(Gstr1Filing filing, SyncCtx ctx) {
        try {
            B2csaApiResponse resp = get("/b2csa", ctx, B2csaApiResponse.class);
            if (resp == null || resp.getB2csa() == null) return 0;

            List<Gstr1B2csa> rows = new ArrayList<>();
            for (var entry : resp.getB2csa()) {
                if (entry.getItms() == null) continue;
                for (var item : entry.getItms()) {
                    rows.add(Gstr1B2csa.builder()
                            .filing(filing)
                            .createdBy(filing.getCreatedBy())
                            .financialYear(filing.getFinancialYear())
                            .originalMonth(entry.getOriginalMonth())
                            .placeOfSupply(entry.getPos())
                            .type(entry.getType())
                            .rate(item.getRt())
                            .taxableValue(z(item.getTxval()))
                            .cessAmount(z(item.getCsamt()))
                            .build());
                }
            }
            return b2csaRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("B2CSA", e);
            return 0;
        }
    }

    private int syncB2cla(Gstr1Filing filing, SyncCtx ctx) {
        try {
            B2claApiResponse resp = get("/b2cla", ctx, B2claApiResponse.class);
            if (resp == null || resp.getB2cla() == null) return 0;

            List<Gstr1B2cla> rows = new ArrayList<>();
            for (var entry : resp.getB2cla()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    if (inv.getItms() == null) continue;
                    for (var item : inv.getItms()) {
                        ItemDetail det = item.getDetail();
                        rows.add(Gstr1B2cla.builder()
                                .filing(filing)
                                .createdBy(filing.getCreatedBy())
                                .originalInvoiceNumber(inv.getOriginalInvoiceNumber())
                                .originalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()))
                                .originalPlaceOfSupply(entry.getPos())
                                .revisedInvoiceNumber(inv.getRevisedInvoiceNumber())
                                .revisedInvoiceDate(parseDate(inv.getRevisedInvoiceDate()))
                                .invoiceValue(inv.getInvoiceValue())
                                .rate(det != null ? det.getRate() : null)
                                .taxableValue(z(det != null ? det.getTaxableValue() : null))
                                .cessAmount(z(det != null ? det.getCsamt() : null))
                                .ecommerceGstin(inv.getEtin())
                                .build());
                    }
                }
            }
            return b2claRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("B2CLA", e);
            return 0;
        }
    }

    private int syncCdnr(Gstr1Filing filing, SyncCtx ctx) {
        try {
            CdnrApiResponse resp = get("/cdnr", ctx, CdnrApiResponse.class);
            if (resp == null || resp.getCdnr() == null) return 0;

            List<Gstr1Cdnr> rows = new ArrayList<>();
            for (var entry : resp.getCdnr()) {
                if (entry.getNt() == null) continue;
                for (var note : entry.getNt()) {
                    if (note.getItms() == null) continue;
                    for (var item : note.getItms()) {
                        ItemDetail det = item.getDetail();
                        rows.add(Gstr1Cdnr.builder()
                                .filing(filing)
                                .createdBy(filing.getCreatedBy())
                                .gstinOfRecipient(upper(entry.getCtin()))
                                .noteType(note.getNoteType())
                                .noteNumber(note.getNoteNumber())
                                .noteDate(parseDate(note.getNoteDate()))
                                .noteValue(z(note.getNoteValue()))
                                .placeOfSupply(note.getPos())
                                .reverseCharge(note.getReverseCharge())
                                .noteSupplyType(note.getInvoiceType())
                                .rate(det != null ? det.getRate() : null)
                                .taxableValue(z(det != null ? det.getTaxableValue() : null))
                                .cessAmount(z(det != null ? det.getCsamt() : null))
                                .build());
                    }
                }
            }
            return cdnrRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("CDNR", e);
            return 0;
        }
    }

    private int syncCdnur(Gstr1Filing filing, SyncCtx ctx) {
        try {
            CdnurApiResponse resp = get("/cdnur", ctx, CdnurApiResponse.class);
            if (resp == null || resp.getCdnur() == null) return 0;

            List<Gstr1Cdnur> rows = new ArrayList<>();
            for (var entry : resp.getCdnur()) {
                if (entry.getItms() == null) continue;
                for (var item : entry.getItms()) {
                    ItemDetail det = item.getDetail();
                    rows.add(Gstr1Cdnur.builder()
                            .filing(filing)
                            .createdBy(filing.getCreatedBy())
                            .urType(entry.getUrType())
                            .noteType(entry.getNoteType())
                            .noteNumber(entry.getNoteNumber())
                            .noteDate(parseDate(entry.getNoteDate()))
                            .noteValue(z(entry.getNoteValue()))
                            .placeOfSupply(entry.getPos())
                            .rate(det != null ? det.getRate() : null)
                            .taxableValue(z(det != null ? det.getTaxableValue() : null))
                            .cessAmount(z(det != null ? det.getCsamt() : null))
                            .build());
                }
            }
            return cdnurRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("CDNUR", e);
            return 0;
        }
    }

    private int syncCdnura(Gstr1Filing filing, SyncCtx ctx) {
        try {
            CdnuraApiResponse resp = get("/cdnura", ctx, CdnuraApiResponse.class);
            if (resp == null || resp.getCdnura() == null) return 0;

            List<Gstr1Cdnura> rows = new ArrayList<>();
            for (var entry : resp.getCdnura()) {
                if (entry.getItms() == null) continue;
                for (var item : entry.getItms()) {
                    ItemDetail det = item.getDetail();
                    rows.add(Gstr1Cdnura.builder()
                            .filing(filing)
                            .createdBy(filing.getCreatedBy())
                            .urType(entry.getUrType())
                            .noteType(entry.getNoteType())
                            .originalNoteNumber(entry.getOriginalNoteNumber())
                            .originalNoteDate(parseDate(entry.getOriginalNoteDate()))
                            .revisedNoteNumber(entry.getRevisedNoteNumber())
                            .revisedNoteDate(parseDate(entry.getRevisedNoteDate()))
                            .noteValue(z(entry.getNoteValue()))
                            .placeOfSupply(entry.getPos())
                            .rate(det != null ? det.getRate() : null)
                            .taxableValue(z(det != null ? det.getTaxableValue() : null))
                            .cessAmount(z(det != null ? det.getCsamt() : null))
                            .build());
                }
            }
            return cdnuraRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("CDNURA", e);
            return 0;
        }
    }

    private int syncExp(Gstr1Filing filing, SyncCtx ctx) {
        try {
            ExpApiResponse resp = get("/exp", ctx, ExpApiResponse.class);
            if (resp == null || resp.getExp() == null) return 0;

            List<Gstr1Exp> rows = new ArrayList<>();
            for (var entry : resp.getExp()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    if (inv.getItms() == null) continue;
                    for (var item : inv.getItms()) {
                        rows.add(Gstr1Exp.builder()
                                .filing(filing)
                                .createdBy(filing.getCreatedBy())
                                .exportType(entry.getExportType())
                                .invoiceNumber(inv.getInvoiceNumber())
                                .invoiceDate(parseDate(inv.getInvoiceDate()))
                                .invoiceValue(inv.getInvoiceValue())
                                .portCode(inv.getPortCode())
                                .shippingBillNumber(inv.getShippingBillNumber())
                                .shippingBillDate(parseDate(inv.getShippingBillDate()))
                                .rate(item.getRt())
                                .taxableValue(z(item.getTxval()))
                                .cessAmount(z(item.getCsamt()))
                                .build());
                    }
                }
            }
            return expRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("EXP", e);
            return 0;
        }
    }

    private int syncExpa(Gstr1Filing filing, SyncCtx ctx) {
        try {
            ExpaApiResponse resp = get("/expa", ctx, ExpaApiResponse.class);
            if (resp == null || resp.getExpa() == null) return 0;

            List<Gstr1Expa> rows = new ArrayList<>();
            for (var entry : resp.getExpa()) {
                if (entry.getInv() == null) continue;
                for (var inv : entry.getInv()) {
                    if (inv.getItms() == null) continue;
                    for (var item : inv.getItms()) {
                        rows.add(Gstr1Expa.builder()
                                .filing(filing)
                                .createdBy(filing.getCreatedBy())
                                .exportType(entry.getExportType())
                                .originalInvoiceNumber(inv.getOriginalInvoiceNumber())
                                .originalInvoiceDate(parseDate(inv.getOriginalInvoiceDate()))
                                .revisedInvoiceNumber(inv.getRevisedInvoiceNumber())
                                .revisedInvoiceDate(parseDate(inv.getRevisedInvoiceDate()))
                                .invoiceValue(inv.getInvoiceValue())
                                .portCode(inv.getPortCode())
                                .shippingBillNumber(inv.getShippingBillNumber())
                                .shippingBillDate(parseDate(inv.getShippingBillDate()))
                                .rate(item.getRt())
                                .taxableValue(z(item.getTxval()))
                                .cessAmount(z(item.getCsamt()))
                                .build());
                    }
                }
            }
            return expaRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("EXPA", e);
            return 0;
        }
    }

    private int syncAt(Gstr1Filing filing, SyncCtx ctx) {
        try {
            AtApiResponse resp = get("/at", ctx, AtApiResponse.class);
            if (resp == null || resp.getAt() == null) return 0;

            List<Gstr1At> rows = new ArrayList<>();
            for (var block : resp.getAt()) {
                rows.addAll(mapAtEntries(block.getAtB2b(),  filing));
                rows.addAll(mapAtEntries(block.getAtB2c(),  filing));
                rows.addAll(mapAtEntries(block.getAtExp(),  filing));
                if (block.getAtEcom() != null) {
                    rows.addAll(mapAtEntries(block.getAtEcom().getEcomUrp2b(), filing));
                    rows.addAll(mapAtEntries(block.getAtEcom().getEcomUrp2c(), filing));
                }
            }
            return atRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("AT", e);
            return 0;
        }
    }

    private List<Gstr1At> mapAtEntries(List<AtApiResponse.AtEntry> entries, Gstr1Filing filing) {
        List<Gstr1At> rows = new ArrayList<>();
        if (entries == null) return rows;
        for (var entry : entries) {
            if (entry.getItms() == null) continue;
            for (var item : entry.getItms()) {
                rows.add(Gstr1At.builder()
                        .filing(filing)
                        .createdBy(filing.getCreatedBy())
                        .placeOfSupply(entry.getPos() != null ? entry.getPos() : "NA")
                        .rate(item.getRt())
                        .grossAdvanceReceived(z(item.getAdAmt()))
                        .cessAmount(BigDecimal.ZERO)
                        .build());
            }
        }
        return rows;
    }

    private int syncTxp(Gstr1Filing filing, SyncCtx ctx) {
        // TXP (advance paid/adjusted) → stored in Gstr1Ata
        try {
            TxpApiResponse resp = get("/txp", ctx, TxpApiResponse.class);
            if (resp == null || resp.getTxpd() == null) return 0;

            List<Gstr1Ata> rows = new ArrayList<>();
            for (var block : resp.getTxpd()) {
                rows.addAll(mapTxpEntries(block.getTxpdB2b(),  filing));
                rows.addAll(mapTxpEntries(block.getTxpdB2c(),  filing));
                rows.addAll(mapTxpEntries(block.getTxpdExp(),  filing));
                if (block.getTxpdEcom() != null) {
                    rows.addAll(mapTxpEntries(block.getTxpdEcom().getEcomUrp2b(), filing));
                    rows.addAll(mapTxpEntries(block.getTxpdEcom().getEcomUrp2c(), filing));
                }
            }
            return ataRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("TXP", e);
            return 0;
        }
    }

    private List<Gstr1Ata> mapTxpEntries(List<TxpApiResponse.TxpEntry> entries, Gstr1Filing filing) {
        List<Gstr1Ata> rows = new ArrayList<>();
        if (entries == null) return rows;
        for (var entry : entries) {
            if (entry.getItms() == null) continue;
            for (var item : entry.getItms()) {
                rows.add(Gstr1Ata.builder()
                        .filing(filing)
                        .createdBy(filing.getCreatedBy())
                        .financialYear(filing.getFinancialYear())
                        .originalMonth(filing.getTaxPeriod())
                        .originalPlaceOfSupply(entry.getPos() != null ? entry.getPos() : "NA")
                        .rate(item.getRt())
                        .grossAdvanceReceived(z(item.getAdAmt()))
                        .cessAmount(BigDecimal.ZERO)
                        .build());
            }
        }
        return rows;
    }

    private int syncNil(Gstr1Filing filing, SyncCtx ctx) {
        // NIL → Gstr1Exemp
        try {
            NilApiResponse resp = get("/nil", ctx, NilApiResponse.class);
            if (resp == null || resp.getNil() == null || resp.getNil().getInv() == null) return 0;

            List<Gstr1Exemp> rows = new ArrayList<>();
            for (var entry : resp.getNil().getInv()) {
                rows.add(Gstr1Exemp.builder()
                        .filing(filing)
                        .createdBy(filing.getCreatedBy())
                        .description(entry.getSupplyType())
                        .nilRatedSupplies(z(entry.getNilAmount()))
                        .exemptedSupplies(z(entry.getExemptedAmount()))
                        .nonGstSupplies(z(entry.getNonGstAmount()))
                        .build());
            }
            return exempRepository.saveAll(rows).size();
        } catch (Exception e) {
            ctx.fail("NIL", e);
            return 0;
        }
    }

    private int[] syncHsn(Gstr1Filing filing, SyncCtx ctx) {
        try {
            HsnApiResponse resp = get("/hsnsum", ctx, HsnApiResponse.class);
            if (resp == null || resp.getHsn() == null) return new int[]{0, 0};

            List<Gstr1HsnB2b> b2bRows = new ArrayList<>();
            List<Gstr1HsnB2c> b2cRows = new ArrayList<>();

            if (resp.getHsn().getHsnB2b() != null) {
                for (var e : resp.getHsn().getHsnB2b()) {
                    b2bRows.add(Gstr1HsnB2b.builder()
                            .filing(filing).createdBy(filing.getCreatedBy())
                            .hsn(e.getHsn()).description(e.getDescription())
                            .uqc(e.getUqc()).totalQuantity(e.getQuantity())
                            .rate(e.getRate()).taxableValue(z(e.getTaxableValue()))
                            .centralTaxAmount(z(e.getCamt()))
                            .stateUtTaxAmount(z(e.getSamt()))
                            .integratedTaxAmount(z(e.getIamt()))
                            .cessAmount(z(e.getCsamt()))
                            .build());
                }
            }
            if (resp.getHsn().getHsnB2c() != null) {
                for (var e : resp.getHsn().getHsnB2c()) {
                    b2cRows.add(Gstr1HsnB2c.builder()
                            .filing(filing).createdBy(filing.getCreatedBy())
                            .hsn(e.getHsn()).description(e.getDescription())
                            .uqc(e.getUqc()).totalQuantity(e.getQuantity())
                            .rate(e.getRate()).taxableValue(z(e.getTaxableValue()))
                            .integratedTaxAmount(z(e.getIamt()))
                            .centralTaxAmount(z(e.getCamt()))
                            .stateUtTaxAmount(z(e.getSamt()))
                            .cessAmount(z(e.getCsamt()))
                            .build());
                }
            }
            return new int[]{
                    hsnB2bRepository.saveAll(b2bRows).size(),
                    hsnB2cRepository.saveAll(b2cRows).size()
            };
        } catch (Exception e) {
            ctx.fail("HSN", e);
            return new int[]{0, 0};
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HTTP HELPER — builds URL, attaches query params + headers
    // ─────────────────────────────────────────────────────────────

    private <T> T get(String path, SyncCtx ctx, Class<T> type) {
        String url = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/gstr1" + path)
                .queryParam("email",     props.getEmail())
                .queryParam("gstin",     ctx.gstin)
                .queryParam("retperiod", ctx.retPeriod)
                .toUriString();
        apiUsageService.recordAndEnforce(ctx.companyGstId, ApiUsageService.GSTR1);
        try {
            return client.getData(url, ctx.headers, type);
        } catch (WhitebooksApiException e) {
            throw gstAuthService.handleCallFailure(ctx.companyGstId, e);
        }
    }

    /** Per-sync state: session headers plus the errors collected from individual sections. */
    private static final class SyncCtx {
        final Integer companyGstId;
        final String gstin;
        final String retPeriod;
        final HttpHeaders headers;
        final List<String> errors = new ArrayList<>();

        SyncCtx(Integer companyGstId, String gstin, String retPeriod, HttpHeaders headers) {
            this.companyGstId = companyGstId;
            this.gstin = gstin;
            this.retPeriod = retPeriod;
            this.headers = headers;
        }

        /**
         * Records a section failure. GSTN answers "no invoices" for empty sections with an error,
         * which is a normal empty result, not a failure. An expired session aborts the whole sync.
         */
        void fail(String section, Exception e) {
            if (e instanceof GstSessionRequiredException sessionEx) throw sessionEx;
            if (e instanceof ApiUsageLimitExceededException limitEx) throw limitEx;
            if (WhitebooksApiException.isNoData(e)) return;
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("GSTR-1 {} sync failed: {}", section, msg);
            errors.add(section + ": " + msg);
        }
    }

    private static final int SECTION_COUNT = 13;

    // ─────────────────────────────────────────────────────────────
    //  CLEANUP — delete all rows before re-sync
    // ─────────────────────────────────────────────────────────────

    private void clearExistingRows(Integer filingId) {
        b2bRepository.deleteByFiling_Id(filingId);
        b2csRepository.deleteByFiling_Id(filingId);
        b2csaRepository.deleteByFiling_Id(filingId);
        b2claRepository.deleteByFiling_Id(filingId);
        cdnrRepository.deleteByFiling_Id(filingId);
        cdnurRepository.deleteByFiling_Id(filingId);
        cdnuraRepository.deleteByFiling_Id(filingId);
        expRepository.deleteByFiling_Id(filingId);
        expaRepository.deleteByFiling_Id(filingId);
        atRepository.deleteByFiling_Id(filingId);
        ataRepository.deleteByFiling_Id(filingId);
        exempRepository.deleteByFiling_Id(filingId);
        hsnB2bRepository.deleteByFiling_Id(filingId);
        hsnB2cRepository.deleteByFiling_Id(filingId);
    }

    // ─────────────────────────────────────────────────────────────
    //  UTILS
    // ─────────────────────────────────────────────────────────────

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, API_DATE); }
        catch (Exception e) { return null; }
    }

    private BigDecimal z(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String upper(String s) {
        return s != null ? s.toUpperCase() : null;
    }
}