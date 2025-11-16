package com.Proyecto.Registro.Repository;

import com.Proyecto.Registro.Entity.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroRepository extends JpaRepository<Registro, Long> {
    // Hereda métodos como save(), findAll(), etc.
}
