import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class ControlVersiones {

    public static void main(String[] args) {
        Repositorio repo = new Repositorio("tienda-web");

        // 1. Ana crea el proyecto
        repo.commit("Ana Torres", "Commit inicial",
                Repositorio.cambios("index.php", "<?php echo 'Bienvenido a la tienda'; ?>"));

        // 2. Luis agrega el login
        repo.commit("Luis Gomez", "Agregar pantalla de login",
                Repositorio.cambios(
                        "login.php", "login v1: valida usuario y clave",
                        "index.php", "<?php echo 'Bienvenido'; include 'login.php'; ?>"));

        // 3. Marta trabaja en su propia rama sin tocar main
        repo.crearRama("feature-carrito");
        repo.cambiarRama("feature-carrito");
        repo.commit("Marta Ruiz", "Agregar carrito de compras",
                Repositorio.cambios("carrito.php", "carrito v1"));

        // 4. Mientras tanto, Ana corrige un bug en main
        repo.cambiarRama("main");
        repo.commit("Ana Torres", "Corregir bug en login (rompia la sesion)",
                Repositorio.cambios("login.php", "login v2: BUG, cierra la sesion por error"));

        // 5. Ana fusiona el trabajo de Marta en main (nadie pierde nada)
        repo.merge("feature-carrito", "Ana Torres");
        repo.log();
        repo.mostrarArchivos();

        // 6. Historial de un archivo y vuelta a una version anterior
        repo.historialArchivo("login.php");
        repo.revertirArchivo("Luis Gomez", "login.php", 2);
        System.out.println("\n>> Luis revirtio login.php a la version del commit 2");
        repo.historialArchivo("login.php");
        repo.mostrarArchivos();

        // 7. Conflicto: dos personas cambian el mismo archivo de forma distinta
        System.out.println("\n>> Marta y Ana editan index.php al mismo tiempo en ramas distintas");
        repo.crearRama("feature-estilos");
        repo.cambiarRama("feature-estilos");
        repo.commit("Marta Ruiz", "Agregar estilos al inicio",
                Repositorio.cambios("index.php", "index con estilos"));
        repo.cambiarRama("main");
        repo.commit("Ana Torres", "Agregar banner al inicio",
                Repositorio.cambios("index.php", "index con banner"));
        repo.merge("feature-estilos", "Ana Torres");   // Git avisa el conflicto

        // 8. Resumen final
        repo.log();
        repo.aportesPorAutor();
        repo.estadoRamas();
    }
}

// ---------------------------------------------------------------------
// COMMIT: una "foto" del proyecto con autor, fecha y mensaje
// ---------------------------------------------------------------------
class Commit {
    final int id;
    final String hash;
    final String autor;
    final String mensaje;
    final LocalDateTime fecha;
    final Commit padre;                  // commit anterior (forma el historial)
    final Commit padre2;                 // solo en commits de merge
    final Map<String, String> archivos;  // estado completo: nombre -> contenido

    Commit(int id, String autor, String mensaje, Commit padre, Commit padre2,
           Map<String, String> archivos) {
        this.id = id;
        this.autor = autor;
        this.mensaje = mensaje;
        this.padre = padre;
        this.padre2 = padre2;
        this.archivos = archivos;
        this.fecha = LocalDateTime.now();
        this.hash = calcularHash();
    }

    private String calcularHash() {
        try {
            String datos = id + autor + mensaje + fecha + archivos;
            byte[] bytes = MessageDigest.getInstance("SHA-1")
                    .digest(datos.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String corto() {
        return hash.substring(0, 7);
    }
}

// ---------------------------------------------------------------------
// REPOSITORIO: guarda commits y ramas
// ---------------------------------------------------------------------
class Repositorio {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String nombre;
    private final Map<Integer, Commit> commits = new LinkedHashMap<>();
    private final Map<String, Commit> ramas = new LinkedHashMap<>(); // rama -> ultimo commit
    private String ramaActual = "main";
    private int contador = 0;

    Repositorio(String nombre) {
        this.nombre = nombre;
        ramas.put("main", null);
    }

    /** Ayuda para armar cambios: cambios("a.txt", "contenido", "b.txt", null) (null = eliminar). */
    static Map<String, String> cambios(String... pares) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) mapa.put(pares[i], pares[i + 1]);
        return mapa;
    }

    // ---- Ramas ------------------------------------------------------

    void crearRama(String rama) {
        if (ramas.containsKey(rama)) throw new IllegalArgumentException("La rama ya existe: " + rama);
        ramas.put(rama, ramas.get(ramaActual));   // nace desde donde estas parado
    }

    void cambiarRama(String rama) {
        if (!ramas.containsKey(rama)) throw new IllegalArgumentException("No existe la rama: " + rama);
        ramaActual = rama;
    }

    // ---- Commits ----------------------------------------------------

    /** Guarda un nuevo commit en la rama actual (equivale a git add + git commit). */
    Commit commit(String autor, String mensaje, Map<String, String> cambios) {
        Commit padre = ramas.get(ramaActual);
        Map<String, String> nuevo = padre == null
                ? new TreeMap<String, String>()
                : new TreeMap<String, String>(padre.archivos);
        for (Map.Entry<String, String> e : cambios.entrySet()) {
            if (e.getValue() == null) nuevo.remove(e.getKey());   // archivo eliminado
            else nuevo.put(e.getKey(), e.getValue());
        }
        return registrar(autor, mensaje, padre, null, nuevo);
    }

    private Commit registrar(String autor, String mensaje, Commit padre, Commit padre2,
                             Map<String, String> archivos) {
        Commit c = new Commit(++contador, autor, mensaje, padre, padre2, archivos);
        commits.put(c.id, c);
        ramas.put(ramaActual, c);      // la rama avanza (como el trigger en MySQL)
        return c;
    }

    // ---- Volver a una version anterior (git revert) -------------------

    /** Crea un commit NUEVO con el contenido antiguo: el historial no se pierde. */
    Commit revertirArchivo(String autor, String archivo, int idCommit) {
        Commit viejo = commits.get(idCommit);
        if (viejo == null || !viejo.archivos.containsKey(archivo))
            throw new IllegalArgumentException("El commit " + idCommit + " no tiene " + archivo);
        return commit(autor, "Revertir " + archivo + " a la version del commit " + idCommit,
                cambios(archivo, viejo.archivos.get(archivo)));
    }

    // ---- Merge (trabajo en equipo) ------------------------------------

    /** Une la rama "origen" dentro de la rama actual. Detecta conflictos. */
    Commit merge(String origen, String autor) {
        Commit destino = ramas.get(ramaActual);
        Commit fuente = ramas.get(origen);
        Commit base = ancestroComun(destino, fuente);
        Map<String, String> archivosBase = base == null ? new TreeMap<String, String>() : base.archivos;

        Set<String> todos = new TreeSet<>();
        todos.addAll(archivosBase.keySet());
        todos.addAll(destino.archivos.keySet());
        todos.addAll(fuente.archivos.keySet());

        Map<String, String> resultado = new TreeMap<>(destino.archivos);
        List<String> conflictos = new ArrayList<>();

        for (String f : todos) {
            String b = archivosBase.get(f), d = destino.archivos.get(f), s = fuente.archivos.get(f);
            if (Objects.equals(s, b)) continue;                       // la otra rama no lo toco
            if (Objects.equals(d, b) || Objects.equals(d, s)) {       // solo cambio una de las ramas
                if (s == null) resultado.remove(f); else resultado.put(f, s);
            } else {
                conflictos.add(f);                                    // ambas lo cambiaron distinto
            }
        }

        if (!conflictos.isEmpty()) {
            System.out.println("  !! CONFLICTO al fusionar '" + origen + "' en '" + ramaActual
                    + "'. Ambas ramas modificaron distinto: " + conflictos);
            System.out.println("  !! No se hizo el merge y no se perdio nada. Hay que resolverlo a mano.");
            return null;
        }
        return registrar(autor, "Merge de " + origen + " en " + ramaActual, destino, fuente, resultado);
    }

    private Set<Integer> ancestros(Commit inicio) {
        Set<Integer> vistos = new HashSet<>();
        Deque<Commit> pila = new ArrayDeque<>();
        if (inicio != null) pila.push(inicio);
        while (!pila.isEmpty()) {
            Commit c = pila.pop();
            if (!vistos.add(c.id)) continue;
            if (c.padre != null) pila.push(c.padre);
            if (c.padre2 != null) pila.push(c.padre2);
        }
        return vistos;
    }

    private Commit ancestroComun(Commit a, Commit b) {
        Set<Integer> deA = ancestros(a);
        Set<Integer> vistos = new HashSet<>();
        Deque<Commit> cola = new ArrayDeque<>();
        if (b != null) cola.add(b);
        while (!cola.isEmpty()) {
            Commit c = cola.poll();
            if (deA.contains(c.id)) return c;
            if (!vistos.add(c.id)) continue;
            if (c.padre != null) cola.add(c.padre);
            if (c.padre2 != null) cola.add(c.padre2);
        }
        return null;
    }

    // ---- Consultas (git log, git status...) ---------------------------

    /** Historial de la rama actual, del mas reciente al mas antiguo (git log). */
    void log() {
        System.out.println("\n== git log | repo: " + nombre + " | rama: " + ramaActual + " ==");
        Commit c = ramas.get(ramaActual);
        while (c != null) {
            System.out.printf("%s %-3d %s | %s | %-11s | %s%n",
                    c.padre2 != null ? "M" : "*", c.id, c.corto(), c.fecha.format(FMT), c.autor, c.mensaje);
            c = c.padre;
        }
    }

    /** Historial de un archivo especifico (git log archivo). */
    void historialArchivo(String archivo) {
        System.out.println("\n== Historial de " + archivo + " (rama " + ramaActual + ") ==");
        Commit c = ramas.get(ramaActual);
        while (c != null) {
            String actual = c.archivos.get(archivo);
            String previo = c.padre == null ? null : c.padre.archivos.get(archivo);
            if (!Objects.equals(actual, previo)) {
                String tipo = previo == null ? "agregado" : (actual == null ? "eliminado" : "modificado");
                System.out.printf("commit %d | %s | %-11s | %-10s | %s%n",
                        c.id, c.fecha.format(FMT), c.autor, tipo, c.mensaje);
            }
            c = c.padre;
        }
    }

    /** Archivos y contenido actuales de la rama. */
    void mostrarArchivos() {
        System.out.println("\n== Archivos actuales (rama " + ramaActual + ") ==");
        Commit c = ramas.get(ramaActual);
        if (c == null) { System.out.println("(vacio)"); return; }
        for (Map.Entry<String, String> e : c.archivos.entrySet())
            System.out.println("- " + e.getKey() + " => " + e.getValue());
    }

    /** Cuantos commits hizo cada autor. */
    void aportesPorAutor() {
        System.out.println("\n== Aportes por autor ==");
        Map<String, Integer> conteo = new TreeMap<>();
        for (Commit c : commits.values()) {
            Integer n = conteo.get(c.autor);
            conteo.put(c.autor, n == null ? 1 : n + 1);
        }
        for (Map.Entry<String, Integer> e : conteo.entrySet())
            System.out.println(e.getKey() + ": " + e.getValue() + " commit(s)");
    }

    /** Estado de cada rama. */
    void estadoRamas() {
        System.out.println("\n== Ramas ==");
        for (Map.Entry<String, Commit> e : ramas.entrySet()) {
            Commit c = e.getValue();
            System.out.printf("%s %-16s -> %s%n", e.getKey().equals(ramaActual) ? "*" : " ",
                    e.getKey(), c == null ? "(sin commits)" : c.corto() + " " + c.mensaje);
        }
    }
}
