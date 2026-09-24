# Control de versiones estilo Git (Java + MySQL)

Ejemplo educativo que modela cómo funciona un sistema de control de versiones como Git: registra cada cambio con **fecha y autor**, permite **volver a versiones anteriores** y hace posible **trabajar en equipo sin borrar el trabajo de otros**.

El mismo concepto se implementa de dos formas:

| Archivo | Qué es |
|---|---|
| `ControlVersiones.java` | Simulación en memoria, sin librerías externas (Java 8 o superior) |
| `control_versiones_git.sql` | Modelo relacional con tablas, trigger, datos de ejemplo y consultas (MySQL 8+) |

## Conceptos que se demuestran

- **Commit:** una "foto" del proyecto con autor, fecha, mensaje y enlace al commit anterior.
- **Ramas:** líneas de trabajo paralelas para que cada persona trabaje sin pisar a las demás.
- **Merge:** unión de ramas, con detección de conflictos cuando dos personas cambian lo mismo.
- **Revert:** volver a una versión anterior creando un commit nuevo, sin perder el historial.
- **Historial:** consulta de quién cambió qué y cuándo, para todo el proyecto o para un archivo.

## Escenario de la demo

Un equipo de tres personas (Ana, Luis y Marta) trabaja en una tienda web:

1. Ana crea el proyecto.
2. Luis agrega el login.
3. Marta desarrolla el carrito en su propia rama, mientras Ana corrige un bug en `main`.
4. Se fusionan las ramas sin perder el trabajo de nadie.
5. Luis revierte `login.php` a una versión anterior.
6. Marta y Ana editan el mismo archivo a la vez y el sistema detecta el conflicto.

## Cómo ejecutar la versión en Java

Requiere tener instalado el JDK.

```bash
javac -encoding UTF-8 ControlVersiones.java
java ControlVersiones
```

Con Java 11 o superior también se puede ejecutar directamente:

```bash
java ControlVersiones.java
```

## Cómo ejecutar la versión en MySQL

Requiere MySQL 8.0 o superior (o MariaDB 10.2.2+).

```bash
mysql -u root -p < control_versiones_git.sql
```

O desde MySQL Workbench: `File > Open SQL Script` y ejecutar todo el script con el rayo sin cursor.

El script recrea la base `control_versiones` desde cero cada vez que se ejecuta.

## Equivalencias con Git real

| Concepto | Java | MySQL | Git |
|---|---|---|---|
| Guardar un cambio | `commit()` | tabla `commits` + `cambios` | `git commit` |
| Historial | `log()` | consulta recursiva | `git log` |
| Crear una rama | `crearRama()` | tabla `ramas` | `git checkout -b` |
| Unir ramas | `merge()` | commit con la rama fusionada | `git merge` |
| Volver atrás | `revertirArchivo()` | commit 6 del script | `git revert` |

## Limitaciones

Es una versión simplificada con fines didácticos. Cada commit guarda el contenido completo de los archivos (Git real guarda diferencias y objetos comprimidos), y en la versión Java los datos viven solo mientras corre el programa.
