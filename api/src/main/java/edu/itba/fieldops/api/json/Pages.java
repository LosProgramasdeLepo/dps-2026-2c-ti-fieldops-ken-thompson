package edu.itba.fieldops.api.json;

import edu.itba.fieldops.usecase.shared.Page;
import edu.itba.fieldops.usecase.shared.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class Pages {
    private Pages() {
    }

    public static PageRequest toRequest(PageQuery query) {
        return new PageRequest(query.page(), query.size());
    }

    public static <T, R> ResponseEntity<PageResponse<R>> toResponse(Page<T> page, Function<? super T, ? extends R> mapper) {
        PageResponse<R> body = new PageResponse<>(
                page.items().stream().<R>map(mapper).toList(),
                page.request().number(),
                page.request().size(),
                page.totalItems(),
                page.totalPages()
        );
        return ResponseEntity.ok().header(HttpHeaders.LINK, links(page)).body(body);
    }

    private static String links(Page<?> page) {
        int number = page.request().number();
        int last = Math.max(page.totalPages() - 1, 0);
        List<String> links = new ArrayList<>();
        links.add(link(page, 0, "first"));
        if (number > 0) {
            links.add(link(page, Math.min(number - 1, last), "prev"));
        }
        if (number < last) {
            links.add(link(page, number + 1, "next"));
        }
        links.add(link(page, last, "last"));
        return String.join(", ", links);
    }

    private static String link(Page<?> page, int number, String relation) {
        String uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page", number)
                .replaceQueryParam("size", page.request().size())
                .toUriString();
        return "<" + uri + ">; rel=\"" + relation + "\"";
    }
}
