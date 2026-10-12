package com.example.backend.service;

import com.example.backend.dto.ConsultaDTO;
import com.example.backend.exception.RecursoNoEncontradoException;
import com.example.backend.mapper.EntityMapper;
import com.example.backend.model.EstadoConsulta;
import com.example.backend.repository.ConsultaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final EntityMapper mapper;

    public ConsultaService(ConsultaRepository consultaRepository, EntityMapper mapper) {
        this.consultaRepository = consultaRepository;
        this.mapper = mapper;
    }

    public Mono<ConsultaDTO.Response> registrar(ConsultaDTO.CrearRequest request) {
        return consultaRepository.save(mapper.toModel(request)).map(mapper::toDTO);
    }

    public Flux<ConsultaDTO.Response> listar() {
        return consultaRepository.findAll().map(mapper::toDTO);
    }

    public Mono<ConsultaDTO.Response> marcarAtendida(String id) {
        return consultaRepository.findById(id)
                .switchIfEmpty(Mono.error(RecursoNoEncontradoException.de("Consulta", id)))
                .flatMap(consulta -> {
                    consulta.setEstado(EstadoConsulta.ATENDIDA);
                    return consultaRepository.save(consulta);
                })
                .map(mapper::toDTO);
    }
}
