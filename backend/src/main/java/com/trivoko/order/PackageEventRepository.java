package com.trivoko.order;

import org.springframework.data.jpa.repository.JpaRepository;

interface PackageEventRepository extends JpaRepository<PackageEvent, Long> {
}
