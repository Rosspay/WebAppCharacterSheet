package com.example.Back.template;

import com.example.Back.template.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class PageResponseTest {

    @Test
    @DisplayName("UT-B-26: total=25, size=10 → totalPages=3")
    void totalPages_roundsUp() {
        var page = PageResponse.of(List.of("a", "b", "c", "d", "e",
                "f", "g", "h", "i", "j"), 25, 0, 10);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.total()).isEqualTo(25);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("UT-B-27: total=0 → totalPages=0")
    void totalPages_zeroTotal() {
        var page = PageResponse.of(List.of(), 0, 0, 10);

        assertThat(page.totalPages()).isZero();
    }

    @Test
    @DisplayName("UT-B-28: size=0 не приводит к делению на ноль")
    void totalPages_zeroSize_noDivisionByZero() {
        var page = PageResponse.of(List.of(), 25, 0, 0);

        assertThat(page.totalPages()).isZero();
    }

    @ParameterizedTest(name = "{0} элементов / по {1} = {2} стр.")
    @DisplayName("UT-B-26 (граничные комбинации): корректность округления вверх")
    @CsvSource({
            "0,   10, 0",
            "1,   10, 1",
            "10,  10, 1",
            "11,  10, 2",
            "100, 25, 4",
            "101, 25, 5"
    })
    void totalPages_boundaryCombinations(long total, int size, long expected) {
        assertThat(PageResponse.of(List.<String>of(), total, 0, size).totalPages())
                .isEqualTo(expected);
    }
}
