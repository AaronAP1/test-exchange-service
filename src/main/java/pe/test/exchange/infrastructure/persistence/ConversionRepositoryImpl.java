package pe.test.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;

@Repository
@RequiredArgsConstructor
public class ConversionRepositoryImpl implements ConversionRepository {

    private final ConversionJpaRepository jpaRepository;

    @Override
    public Conversion guardar(Conversion conversion) {
        return jpaRepository.save(conversion);
    }
}
