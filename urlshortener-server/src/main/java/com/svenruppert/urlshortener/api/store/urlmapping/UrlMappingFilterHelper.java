package com.svenruppert.urlshortener.api.store.urlmapping;

import com.svenruppert.urlshortener.core.urlmapping.ShortUrlMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.svenruppert.urlshortener.api.store.urlmapping.UrlMappingFilter.Direction.ASC;
import static com.svenruppert.urlshortener.api.store.urlmapping.UrlMappingFilter.Direction.DESC;
import static com.svenruppert.urlshortener.core.AliasPolicy.normalize;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Comparator.*;

public class UrlMappingFilterHelper {
  public static final Comparator<ShortUrlMapping> NO_OP_COMPARATOR = (a, b) -> 0;

  private UrlMappingFilterHelper() {
  }

  public static boolean matches(UrlMappingFilter f, ShortUrlMapping m) {
    return matchActive(f, m) && matchCode(f, m) && matchUrl(f, m) && matchCreated(f, m);
  }

  private static boolean matchActive(UrlMappingFilter f, ShortUrlMapping m) {
    var active = f.active();
    if (active.isEmpty()) return true;
    var aBoolean = active.get();
    if (aBoolean) {
      return m.active();
    } else {
      return !m.active();
    }
  }

  public static List<ShortUrlMapping> filterSortAndPage(UrlMappingFilter filter, Stream<ShortUrlMapping> valueStream) {
    List<ShortUrlMapping> valuesFilteredAndSorted = valueStream
        .filter(mapping -> matches(filter, mapping))
        .sorted(buildComparator(filter))
        .collect(Collectors.toList());

    // 3) Paging
    int from = Math.max(0, filter.offset().orElse(0));
    int lim = filter.limit().orElse(MAX_VALUE);
    if (from >= valuesFilteredAndSorted.size()) return List.of();
    int to = Math.min(valuesFilteredAndSorted.size(), from + Math.max(0, lim));
    return valuesFilteredAndSorted.subList(from, to);
  }


  private static boolean matchCode(UrlMappingFilter f, ShortUrlMapping m) {
    return f.codePart()
        .map(part -> {
          String code = normalize(m.shortCode());
          return code.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
        })
        .orElse(true);
  }

  private static boolean matchUrl(UrlMappingFilter f, ShortUrlMapping m) {
    return f.urlPart()
        .map(part -> {
          String url = m.originalUrl();
          if (url == null) return false;
          return url.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
        })
        .orElse(true);
  }

  private static boolean matchCreated(UrlMappingFilter f, ShortUrlMapping m) {
    var created = m.createdAt();
    if (f.createdFrom().isPresent() && created.isBefore(f.createdFrom().get())) return false;
    if (f.createdTo().isPresent() && created.isAfter(f.createdTo().get())) return false;
    return true;
  }

  private static Comparator<ShortUrlMapping> buildComparator(UrlMappingFilter filter) {
    if (filter == null) return NO_OP_COMPARATOR;

    Comparator<ShortUrlMapping> base = filter
        .sortBy()
        .map(sb -> switch (sb) {
          case CREATED_AT -> comparing(ShortUrlMapping::createdAt, nullsFirst(naturalOrder()));
          case SHORT_CODE -> comparing(ShortUrlMapping::shortCode, nullsFirst(naturalOrder()));
          case ORIGINAL_URL -> comparing(ShortUrlMapping::originalUrl, nullsFirst(naturalOrder()));
          case EXPIRES_AT -> comparing(
              (ShortUrlMapping m) -> m.expiresAt().orElse(null),
              nullsLast(naturalOrder())
          );
        })
        .orElse(NO_OP_COMPARATOR);

    Comparator<ShortUrlMapping> cmp =
        base.thenComparing(ShortUrlMapping::shortCode, nullsFirst(naturalOrder()));

    return filter.direction().orElse(ASC) == DESC ? cmp.reversed() : cmp;
  }
}