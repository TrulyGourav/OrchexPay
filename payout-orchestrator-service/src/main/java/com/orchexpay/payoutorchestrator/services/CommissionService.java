package com.orchexpay.payoutorchestrator.services;

import com.orchexpay.payoutorchestrator.enums.CommissionType;
import com.orchexpay.payoutorchestrator.models.MerchantCommission;
import com.orchexpay.payoutorchestrator.repositories.MerchantCommissionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Scalable commission logic: computes platform (merchant) share from order amount.
 * Default: 0% if no config; add new CommissionType without breaking existing.
 */
@Service
public class CommissionService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final MerchantCommissionRepository commissionRepository;

    public CommissionService(MerchantCommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    /**
     * Platform share (commission) for the given order amount. Vendor share = orderAmount - platformShare.
     */
    public BigDecimal computePlatformShare(UUID merchantId, BigDecimal orderAmount, String currencyCode) {
        return commissionRepository.findByMerchantId(merchantId)
                .map(config -> compute(config, orderAmount, currencyCode))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Vendor share = orderAmount - platformShare. Rounded to scale.
     */
    public BigDecimal computeVendorShare(UUID merchantId, BigDecimal orderAmount, String currencyCode) {
        BigDecimal platformShare = computePlatformShare(merchantId, orderAmount, currencyCode);
        return orderAmount.subtract(platformShare).setScale(SCALE, ROUNDING);
    }

    private BigDecimal compute(MerchantCommission config, BigDecimal orderAmount, String currencyCode) {
        if (config.getCommissionType() == CommissionType.PERCENTAGE) {
            return orderAmount
                    .multiply(config.getPercentageValue())
                    .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
        }
        if (config.getCommissionType() == CommissionType.FIXED_PLUS_PERCENTAGE) {
            BigDecimal fixed = config.getFixedAmount() != null ? config.getFixedAmount() : BigDecimal.ZERO;
            BigDecimal pct = orderAmount
                    .multiply(config.getPercentageValue())
                    .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
            return fixed.add(pct).setScale(SCALE, ROUNDING);
        }
        return BigDecimal.ZERO;
    }
}
