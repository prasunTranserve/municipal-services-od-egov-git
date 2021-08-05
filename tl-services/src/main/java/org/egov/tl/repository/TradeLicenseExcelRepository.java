package org.egov.tl.repository;

import org.egov.tl.web.models.excel.TradeLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeLicenseExcelRepository extends JpaRepository<TradeLicense, String> {

}
