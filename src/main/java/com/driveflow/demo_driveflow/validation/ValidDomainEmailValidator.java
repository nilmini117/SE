package com.driveflow.demo_driveflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Validates email addresses by:
 * 1. Performing client/server-side regex syntax verification.
 * 2. Performing a DNS query via JNDI to verify that the domain has active MX (Mail Exchange) records.
 */
public class ValidDomainEmailValidator implements ConstraintValidator<ValidDomainEmail, String> {

    private static final Logger log = LoggerFactory.getLogger(ValidDomainEmailValidator.class);

    private static final Pattern EMAIL_SYNTAX_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    // Whitelist for internal integration seed / demo mock domains during offline test runs
    private static final Set<String> WHITELISTED_TEST_DOMAINS = Set.of(
            "driveflow.com",
            "precisionfleet.com",
            "apexfleet.com",
            "volttechfleet.com",
            "example.com",
            "test.com",
            "localhost"
    );

    // In-memory cache to avoid repeated DNS queries for common domains (e.g. gmail.com, outlook.com)
    private static final Map<String, Boolean> DOMAIN_CACHE = new ConcurrentHashMap<>();

    @Override
    public void initialize(ValidDomainEmail constraintAnnotation) {
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // Let @NotBlank / @NotNull handle null or empty values if combined
        if (email == null || email.trim().isEmpty()) {
            return true;
        }

        String trimmed = email.trim();

        // 1. Syntactic regex check
        if (!EMAIL_SYNTAX_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // 2. Extract domain
        int atIndex = trimmed.lastIndexOf('@');
        if (atIndex == -1 || atIndex == trimmed.length() - 1) {
            return false;
        }

        String domain = trimmed.substring(atIndex + 1).toLowerCase();

        // Ensure domain has no illegal trailing dots or invalid parts
        if (domain.startsWith(".") || domain.endsWith(".") || !domain.contains(".")) {
            return false;
        }

        // 3. Whitelisted seed / demo domain check
        if (WHITELISTED_TEST_DOMAINS.contains(domain)) {
            return true;
        }

        // 4. DNS MX record verification
        return verifyDomainMxRecord(domain);
    }

    /**
     * Queries DNS for MX records of the specified domain using JNDI.
     */
    public static boolean verifyDomainMxRecord(String domain) {
        if (DOMAIN_CACHE.containsKey(domain)) {
            return DOMAIN_CACHE.get(domain);
        }

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");

            DirContext ctx = new InitialDirContext(env);
            Attributes attributes = ctx.getAttributes(domain, new String[]{"MX"});
            Attribute mxAttr = attributes.get("MX");

            boolean hasMx = (mxAttr != null && mxAttr.size() > 0);
            DOMAIN_CACHE.put(domain, hasMx);
            return hasMx;
        } catch (NamingException e) {
            log.info("DNS MX lookup failed for domain '{}': {}", domain, e.getMessage());
            DOMAIN_CACHE.put(domain, false);
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error during DNS MX lookup for domain '{}': {}", domain, e.getMessage());
            DOMAIN_CACHE.put(domain, false);
            return false;
        }
    }

    /**
     * Clear domain cache (useful for testing).
     */
    public static void clearCache() {
        DOMAIN_CACHE.clear();
    }
}
