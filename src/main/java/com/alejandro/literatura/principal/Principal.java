package com.alejandro.literatura.principal;

import com.alejandro.literatura.model.*;
import com.alejandro.literatura.repository.AutorRepository;
import com.alejandro.literatura.repository.LibroRepository;
import com.alejandro.literatura.service.ConsumoAPI;
import com.alejandro.literatura.service.ConvierteDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Component
public class Principal {

    private static final String URL_BASE = "https://gutendex.com/books/?search=";

    private final Scanner teclado = new Scanner(System.in);
    private final ConsumoAPI consumoAPI = new ConsumoAPI();
    private final ConvierteDatos conversor = new ConvierteDatos();

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    public void muestraElMenu() {
        int opcion = -1;

        while (opcion != 0) {
            String menu = """
                    
                    ╔══════════════════════════════════════╗
                    ║      📚 CATÁLOGO DE LIBROS 📚        ║
                    ╠══════════════════════════════════════╣
                    ║  1 - Buscar libro por título         ║
                    ║  2 - Listar libros registrados       ║
                    ║  3 - Listar autores registrados      ║
                    ║  4 - Listar autores vivos en un año  ║
                    ║  5 - Listar libros por idioma        ║
                    ║  0 - Salir                           ║
                    ╚══════════════════════════════════════╝
                    Elige una opción:""";

            System.out.println(menu);

            try {
                opcion = Integer.parseInt(teclado.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("⚠️  Opción inválida. Por favor ingresa un número.");
                continue;
            }

            switch (opcion) {
                case 1 -> buscarLibroPorTitulo();
                case 2 -> listarLibrosRegistrados();
                case 3 -> listarAutoresRegistrados();
                case 4 -> listarAutoresVivosEnAnio();
                case 5 -> listarLibrosPorIdioma();
                case 0 -> System.out.println("\n👋 ¡Hasta luego! Gracias por usar el Catálogo de Libros.");
                default -> System.out.println("⚠️  Opción no válida. Elige entre 0 y 5.");
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // OPCIÓN 1: Buscar libro por título en la API y guardar en BD
    // ──────────────────────────────────────────────────────────
    private void buscarLibroPorTitulo() {
        System.out.println("\n🔍 Ingresa el título del libro a buscar:");
        String tituloBuscado = teclado.nextLine().trim();

        if (tituloBuscado.isBlank()) {
            System.out.println("⚠️  El título no puede estar vacío.");
            return;
        }

        // Verificar si ya está registrado en la BD
        Optional<Libro> libroExistente = libroRepository.findByTituloContainingIgnoreCase(tituloBuscado);
        if (libroExistente.isPresent()) {
            System.out.println("\n✅ El libro ya se encuentra en la base de datos:");
            System.out.println(libroExistente.get());
            return;
        }

        // Consumir la API de Gutendex
        try {
            String urlBusqueda = URL_BASE + tituloBuscado.replace(" ", "%20");
            String json = consumoAPI.obtenerDatos(urlBusqueda);
            RespuestaAPI respuesta = conversor.obtenerDatos(json, RespuestaAPI.class);

            if (respuesta.resultados() == null || respuesta.resultados().isEmpty()) {
                System.out.println("❌ No se encontró ningún libro con ese título en Gutendex.");
                return;
            }

            // Tomar el primer resultado
            DatosLibro datosLibro = respuesta.resultados().get(0);

            // Crear y guardar el libro
            Libro libro = new Libro();
            libro.setTitulo(datosLibro.titulo());
            libro.setIdioma(datosLibro.idiomas() != null && !datosLibro.idiomas().isEmpty()
                    ? datosLibro.idiomas().get(0) : "Desconocido");
            libro.setNumeroDescargas(datosLibro.numeroDescargas());

            // Procesar autores
            if (datosLibro.autores() != null) {
                for (DatosAutor datosAutor : datosLibro.autores()) {
                    // Buscar si el autor ya existe
                    Optional<Autor> autorExistente = autorRepository.findByNombreContainingIgnoreCase(datosAutor.nombre());
                    Autor autor;
                    if (autorExistente.isPresent()) {
                        autor = autorExistente.get();
                    } else {
                        autor = new Autor(datosAutor.nombre(), datosAutor.anioNacimiento(), datosAutor.anioFallecimiento());
                        autorRepository.save(autor);
                    }
                    libro.getAutores().add(autor);
                }
            }

            libroRepository.save(libro);

            System.out.println("\n✅ ¡Libro registrado exitosamente!");
            System.out.println(libro);

        } catch (Exception e) {
            System.out.println("❌ Error al buscar el libro: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────
    // OPCIÓN 2: Listar todos los libros registrados en la BD
    // ──────────────────────────────────────────────────────────
    private void listarLibrosRegistrados() {
        List<Libro> libros = libroRepository.findAll();

        if (libros.isEmpty()) {
            System.out.println("\n📭 No hay libros registrados en la base de datos.");
            return;
        }

        System.out.println("\n📚 Libros registrados (" + libros.size() + "):");
        libros.forEach(System.out::println);
    }

    // ──────────────────────────────────────────────────────────
    // OPCIÓN 3: Listar todos los autores registrados
    // ──────────────────────────────────────────────────────────
    private void listarAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAll();

        if (autores.isEmpty()) {
            System.out.println("\n📭 No hay autores registrados en la base de datos.");
            return;
        }

        System.out.println("\n✍️  Autores registrados (" + autores.size() + "):");
        autores.forEach(System.out::println);
    }

    // ──────────────────────────────────────────────────────────
    // OPCIÓN 4: Listar autores vivos en un año determinado
    // ──────────────────────────────────────────────────────────
    private void listarAutoresVivosEnAnio() {
        System.out.println("\n📅 Ingresa el año para buscar autores vivos:");
        String inputAnio = teclado.nextLine().trim();

        int anio;
        try {
            anio = Integer.parseInt(inputAnio);
        } catch (NumberFormatException e) {
            System.out.println("⚠️  Por favor ingresa un año válido (número entero).");
            return;
        }

        List<Autor> autores = autorRepository.findAutoresVivosEnAnio(anio);

        if (autores.isEmpty()) {
            System.out.println("\n📭 No se encontraron autores registrados vivos en el año " + anio + ".");
            return;
        }

        System.out.println("\n✍️  Autores vivos en " + anio + " (" + autores.size() + "):");
        autores.forEach(System.out::println);
    }

    // ──────────────────────────────────────────────────────────
    // OPCIÓN 5: Listar libros por idioma
    // ──────────────────────────────────────────────────────────
    private void listarLibrosPorIdioma() {
        System.out.println("""
                
                🌍 Idiomas disponibles:
                  es - Español
                  en - Inglés
                  fr - Francés
                  pt - Portugués
                  de - Alemán
                  it - Italiano
                  
                Ingresa el código del idioma:""");

        String idioma = teclado.nextLine().trim().toLowerCase();

        if (idioma.isBlank()) {
            System.out.println("⚠️  El idioma no puede estar vacío.");
            return;
        }

        List<Libro> libros = libroRepository.findByIdioma(idioma);

        if (libros.isEmpty()) {
            System.out.println("\n📭 No hay libros registrados en ese idioma: \"" + idioma + "\"");
            return;
        }

        System.out.println("\n📚 Libros en idioma \"" + idioma + "\" (" + libros.size() + "):");
        libros.forEach(System.out::println);
    }
}
