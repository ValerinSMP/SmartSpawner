# Modificaciones de ValerinSMP

Esta rama conserva cambios locales de ValerinSMP respecto al proyecto original.
Fecha de publicación de esta revisión: 2026-10-09.

No es una distribución oficial del autor original. Se mantienen su atribución,
historial y licencia. El repositorio padre de GitHub identifica el upstream.
Los cambios concretos constan en los commits y diferencias de esta rama.

No se publican credenciales de producción, bases de datos operativas ni JAR de
dependencias comerciales. Los archivos locales necesarios para compilar que no
sean redistribuibles deben obtenerse por los canales originales autorizados.

Publicación de fuentes no significa que esta revisión haya sido compilada,
instalada o validada en un servidor. No usarla para sustituir producción sin una
validación separada.

## Reglas recuperadas tras merge upstream (2026-10-09)

La copia Git inicial de personal no contenía los dos parches históricos indicados
por el propietario. Se recuperó su comportamiento sobre upstream 08f4e8b usando
como evidencia la referencia antigua SmartSpawner-live ya existente en HomeSV;
no se decompilaron nuevos JAR ni se sustituyeron clases del upstream por archivos antiguos.

- XP: reconocer las claves mending, repairing y repair, además de Mending vanilla,
  en los encantamientos reales del item. Conserva el cálculo/reparación y callbacks
  del flujo actual; no introduce dependencia obligatoria de EcoEnchants.
- Huevos: spawner_egg_change.max_stack_to_allow_change, default 1. Bloqueo antes
  de evento/cambio/consumo para stacks mayores; <= 0 desactiva el límite como
  en la referencia anterior. Mensaje egg_change_stacked_not_allowed configurable.
- Conservado el fix upstream: un evento de cambio cancelado no consume el huevo.
- Cuatro regresiones de claves y límites; añadido el módulo API al classpath
  de tests porque upstream había aislado esas dependencias.
- Compilación y 86 tests aprobados. Sin instalación ni prueba gameplay.
