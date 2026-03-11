package com.alejandro.literatura.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "libros")
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String titulo;

    private String idioma;
    private Integer numeroDescargas;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(
        name = "libro_autor",
        joinColumns = @JoinColumn(name = "libro_id"),
        inverseJoinColumns = @JoinColumn(name = "autor_id")
    )
    private List<Autor> autores = new ArrayList<>();

    // Constructor vacío requerido por JPA
    public Libro() {}

    public Libro(String titulo, String idioma, Integer numeroDescargas) {
        this.titulo = titulo;
        this.idioma = idioma;
        this.numeroDescargas = numeroDescargas;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public Integer getNumeroDescargas() { return numeroDescargas; }
    public void setNumeroDescargas(Integer numeroDescargas) { this.numeroDescargas = numeroDescargas; }

    public List<Autor> getAutores() { return autores; }
    public void setAutores(List<Autor> autores) { this.autores = autores; }

    @Override
    public String toString() {
        String autoresStr = autores.isEmpty() ? "Desconocido"
                : autores.stream().map(Autor::getNombre).reduce((a, b) -> a + ", " + b).orElse("N/A");

        return String.format("""
                ╔══════════════════════════════╗
                  LIBRO
                  Título: %s
                  Autor(es): %s
                  Idioma: %s
                  Número de descargas: %s
                ╚══════════════════════════════╝""",
                titulo, autoresStr, idioma,
                numeroDescargas != null ? numeroDescargas : "N/A");
    }
}
