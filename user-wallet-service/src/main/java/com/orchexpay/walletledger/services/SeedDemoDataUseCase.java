package com.orchexpay.walletledger.services;

import com.orchexpay.walletledger.exceptions.UserAlreadyExistsException;
import com.orchexpay.walletledger.repositories.UserRepository;
import com.orchexpay.walletledger.enums.Role;
import com.orchexpay.walletledger.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds demo data: 11 merchants and 6-8 vendors per merchant (same data as scripts/seed_demo_data.py).
 * Idempotent: skips creation when username already exists.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeedDemoDataUseCase {

    private static final String DEFAULT_PASSWORD = "password";
    private static final String CURRENCY = "INR";

    private static final List<String> MERCHANTS = List.of(
            "spicekitchen_in",
            "freshbites_mumbai",
            "urbanbakers_delhi",
            "greenleaf_chennai",
            "coastalfood_bangalore",
            "northstar_jaipur",
            "metro_cafe_pune",
            "dailygrub_hyderabad",
            "homestyle_kolkata",
            "quickbite_ahmedabad",
            "thefoodhub_lucknow"
    );

    private static final Map<String, List<String>> VENDORS_BY_MERCHANT = Map.ofEntries(
            Map.entry("spicekitchen_in", List.of(
                    "spicekitchen_biryani", "spicekitchen_curries", "spicekitchen_tandoor",
                    "spicekitchen_south", "spicekitchen_sweets", "spicekitchen_chaat", "spicekitchen_grill")),
            Map.entry("freshbites_mumbai", List.of(
                    "freshbites_sandwich", "freshbites_salads", "freshbites_smoothies", "freshbites_breakfast",
                    "freshbites_wraps", "freshbites_jucebar", "freshbites_desserts", "freshbites_snacks")),
            Map.entry("urbanbakers_delhi", List.of(
                    "urbanbakers_bread", "urbanbakers_pastries", "urbanbakers_cakes", "urbanbakers_cookies",
                    "urbanbakers_savory", "urbanbakers_artisan", "urbanbakers_continental")),
            Map.entry("greenleaf_chennai", List.of(
                    "greenleaf_vegan", "greenleaf_south_indian", "greenleaf_fresh", "greenleaf_salads",
                    "greenleaf_soups", "greenleaf_smoothies", "greenleaf_organic")),
            Map.entry("coastalfood_bangalore", List.of(
                    "coastalfood_seafood", "coastalfood_mangalorean", "coastalfood_grill", "coastalfood_curries",
                    "coastalfood_rice", "coastalfood_fry", "coastalfood_special")),
            Map.entry("northstar_jaipur", List.of(
                    "northstar_rajasthani", "northstar_dal_bati", "northstar_sweets", "northstar_street",
                    "northstar_thali", "northstar_chaat", "northstar_desserts")),
            Map.entry("metro_cafe_pune", List.of(
                    "metro_cafe_coffee", "metro_cafe_brunch", "metro_cafe_bakery", "metro_cafe_sandwich",
                    "metro_cafe_pasta", "metro_cafe_desserts", "metro_cafe_quick")),
            Map.entry("dailygrub_hyderabad", List.of(
                    "dailygrub_biryani", "dailygrub_haleem", "dailygrub_curries", "dailygrub_tiffin",
                    "dailygrub_snacks", "dailygrub_sweets", "dailygrub_grill")),
            Map.entry("homestyle_kolkata", List.of(
                    "homestyle_bengali", "homestyle_fish", "homestyle_sweets", "homestyle_vegetarian",
                    "homestyle_rice", "homestyle_snacks", "homestyle_tea")),
            Map.entry("quickbite_ahmedabad", List.of(
                    "quickbite_gujarati", "quickbite_farsan", "quickbite_thali", "quickbite_street",
                    "quickbite_sweets", "quickbite_snacks")),
            Map.entry("thefoodhub_lucknow", List.of(
                    "thefoodhub_awadhi", "thefoodhub_kebabs", "thefoodhub_biryani", "thefoodhub_bread",
                    "thefoodhub_sweets", "thefoodhub_street", "thefoodhub_chaat", "thefoodhub_grill"))
    );

    private final UserRepository userRepository;
    private final CreateUserUseCase createUserUseCase;
    private final AddVendorUseCase addVendorUseCase;

    public record SeedResult(int merchantsCreated, int vendorsCreated) {}

    @Transactional(rollbackFor = Exception.class)
    public SeedResult execute() {
        int merchantsCreated = 0;
        int vendorsCreated = 0;

        for (String merchantUsername : MERCHANTS) {
            UUID merchantId;
            if (userRepository.existsByUsername(merchantUsername)) {
                merchantId = userRepository.findByUsername(merchantUsername)
                        .map(User::getMerchantId)
                        .orElse(null);
                if (merchantId == null) {
                    log.warn("User {} exists but has no merchantId, skipping vendors", merchantUsername);
                    continue;
                }
            } else {
                try {
                    var result = createUserUseCase.execute(
                            merchantUsername,
                            DEFAULT_PASSWORD,
                            Set.of(Role.MERCHANT),
                            CURRENCY
                    );
                    merchantId = result.user().getMerchantId();
                    merchantsCreated++;
                } catch (UserAlreadyExistsException e) {
                    merchantId = userRepository.findByUsername(merchantUsername)
                            .map(User::getMerchantId)
                            .orElse(null);
                    if (merchantId == null) continue;
                }
            }

            List<String> vendorUsernames = VENDORS_BY_MERCHANT.getOrDefault(merchantUsername, List.of());
            for (String vendorUsername : vendorUsernames) {
                if (userRepository.existsByUsername(vendorUsername)) continue;
                try {
                    addVendorUseCase.execute(merchantId, vendorUsername, DEFAULT_PASSWORD, CURRENCY);
                    vendorsCreated++;
                } catch (UserAlreadyExistsException ignored) {
                    // race or already created
                }
            }
        }

        log.info("Seed demo data: merchantsCreated={}, vendorsCreated={}", merchantsCreated, vendorsCreated);
        return new SeedResult(merchantsCreated, vendorsCreated);
    }
}
