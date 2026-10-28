package pe.test.exchange.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.test.exchange.domain.model.Conversion;

public interface ConversionJpaRepository extends JpaRepository<Conversion, Long> {
}
