package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.CrmQueryRequest.FilterRule;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a react-querybuilder RuleGroup into a JPA {@link Specification}.
 *
 * Supports:
 *   - String operators: =, !=, contains, beginsWith, endsWith, null, notNull
 *   - Number operators: =, !=, <, <=, >, >=
 *   - Date operators:   =, !=, <, <=, >, >=
 *   - Boolean operators: =
 *   - Dot-notation field paths for JOINs: e.g. "organization.industry"
 *   - Nested rule groups (recursive)
 *
 * @param <T> the JPA entity type to build a Specification for
 */
@Component
public class CrmSpecificationBuilder {

    public <T> Specification<T> build(String combinator, List<FilterRule> rules) {
        return (root, query, cb) -> {
            if (rules == null || rules.isEmpty()) return cb.conjunction();
            return buildPredicate(root, cb, combinator, rules);
        };
    }

    private <T> Predicate buildPredicate(
            Root<T> root, CriteriaBuilder cb,
            String combinator, List<FilterRule> rules) {

        List<Predicate> predicates = new ArrayList<>();

        for (FilterRule rule : rules) {
            if (rule.getRules() != null && !rule.getRules().isEmpty()) {
                // Nested group — recurse
                predicates.add(buildPredicate(root, cb,
                        rule.getCombinator() != null ? rule.getCombinator() : "and",
                        rule.getRules()));
            } else {
                Predicate p = toPredicate(root, cb, rule);
                if (p != null) predicates.add(p);
            }
        }

        if (predicates.isEmpty()) return cb.conjunction();

        Predicate[] arr = predicates.toArray(new Predicate[0]);
        return "or".equalsIgnoreCase(combinator) ? cb.or(arr) : cb.and(arr);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Predicate toPredicate(Root<T> root, CriteriaBuilder cb, FilterRule rule) {
        if (rule.getField() == null || rule.getOperator() == null) return null;

        Path<?> path = resolvePath(root, rule.getField());
        if (path == null) return null;

        String op  = rule.getOperator().toLowerCase();
        String val = rule.getValue();

        // ── Null checks ──────────────────────────────────────────────────
        if ("null".equals(op))    return cb.isNull(path);
        if ("notNull".equals(op)) return cb.isNotNull(path);

        if (val == null) return null;

        // ── Type-aware comparison ────────────────────────────────────────
        Class<?> javaType = path.getJavaType();

        if (String.class.isAssignableFrom(javaType)) {
            return stringPredicate(cb, (Path<String>) path, op, val);
        }

        if (Number.class.isAssignableFrom(javaType) || isPrimitive(javaType)) {
            return numberPredicate(cb, path, op, val, javaType);
        }

        if (Boolean.class.isAssignableFrom(javaType) || boolean.class == javaType) {
            boolean boolVal = "true".equalsIgnoreCase(val) || "1".equals(val);
            return "!=".equals(op)
                    ? cb.notEqual(path, boolVal)
                    : cb.equal(path, boolVal);
        }

        if (LocalDate.class.isAssignableFrom(javaType)) {
            return datePredicate(cb, (Path<LocalDate>) path, op, val);
        }

        // Enum or other — treat as string
        return cb.equal(path.as(String.class), val);
    }

    // ── Field path resolution (supports "organization.industry") ────────

    private <T> Path<?> resolvePath(Root<T> root, String field) {
        try {
            String[] parts = field.split("\\.");
            Path<?> path = root;
            for (int i = 0; i < parts.length; i++) {
                if (i < parts.length - 1) {
                    // Intermediate segment — must be a join (many-to-one / one-to-one)
                    path = ((Root<T>) root).join(parts[i], JoinType.LEFT);
                    root = null; // after first join, path is a Join
                } else {
                    path = path.get(parts[i]);
                }
            }
            return path;
        } catch (Exception e) {
            return null; // unknown field — skip predicate
        }
    }

    // ── String predicates ────────────────────────────────────────────────

    private Predicate stringPredicate(CriteriaBuilder cb, Path<String> path, String op, String val) {
        return switch (op) {
            case "contains"     -> cb.like(cb.lower(path), "%" + val.toLowerCase() + "%");
            case "doesNotContain" -> cb.notLike(cb.lower(path), "%" + val.toLowerCase() + "%");
            case "beginsWith"   -> cb.like(cb.lower(path), val.toLowerCase() + "%");
            case "endsWith"     -> cb.like(cb.lower(path), "%" + val.toLowerCase());
            case "!="           -> cb.notEqual(cb.lower(path), val.toLowerCase());
            default             -> cb.equal(cb.lower(path), val.toLowerCase());
        };
    }

    // ── Number predicates ────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate numberPredicate(CriteriaBuilder cb, Path<?> path, String op, String val, Class<?> javaType) {
        try {
            Comparable num = parseNumber(val, javaType);
            Path<Comparable> comparablePath = (Path<Comparable>) path;
            return switch (op) {
                case "!="  -> cb.notEqual(path, num);
                case "<"   -> cb.lessThan(comparablePath, num);
                case "<="  -> cb.lessThanOrEqualTo(comparablePath, num);
                case ">"   -> cb.greaterThan(comparablePath, num);
                case ">="  -> cb.greaterThanOrEqualTo(comparablePath, num);
                default    -> cb.equal(path, num);
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Comparable<?> parseNumber(String val, Class<?> javaType) {
        if (Integer.class == javaType || int.class == javaType) return Integer.parseInt(val);
        if (Long.class == javaType    || long.class == javaType) return Long.parseLong(val);
        if (BigDecimal.class == javaType) return new BigDecimal(val);
        if (Double.class == javaType  || double.class == javaType) return Double.parseDouble(val);
        if (Float.class == javaType   || float.class == javaType) return Float.parseFloat(val);
        return new BigDecimal(val);
    }

    // ── Date predicates ──────────────────────────────────────────────────

    private Predicate datePredicate(CriteriaBuilder cb, Path<LocalDate> path, String op, String val) {
        try {
            LocalDate date = LocalDate.parse(val);
            return switch (op) {
                case "!="  -> cb.notEqual(path, date);
                case "<"   -> cb.lessThan(path, date);
                case "<="  -> cb.lessThanOrEqualTo(path, date);
                case ">"   -> cb.greaterThan(path, date);
                case ">="  -> cb.greaterThanOrEqualTo(path, date);
                default    -> cb.equal(path, date);
            };
        } catch (Exception e) {
            return null;
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private boolean isPrimitive(Class<?> c) {
        return c == int.class || c == long.class || c == double.class
                || c == float.class || c == short.class || c == byte.class;
    }
}
