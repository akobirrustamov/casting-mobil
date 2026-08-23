package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Enums.CurrencyKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurrencyPackageRepo extends JpaRepository<CurrencyPackage, Long> {

    List<CurrencyPackage> findAllByOrderByKindAscSortOrderAsc();

    List<CurrencyPackage> findAllByKindOrderBySortOrderAsc(CurrencyKind kind);
}
