# D6 — colocador y protecciones superpuestas

Política de suma entre tipos sustituida por D7: [cupos por tipo](DELIVERY-D7-PER-TYPE-20261011.md).
El resto de D6 se conserva; D7 es la entrega actual.

Fuente canónica personal: SmartSpawner, rama work/valerin-production-contract-v1.
Destino autorizado: Custom, JAR en disco para reinicio diario del propietario. Sin lifecycle.

## Comportamiento
Un bloque nuevo pertenece al UUID del jugador que lo coloca; no al propietario del terreno
ni al dueño anterior guardado en el objeto. La retirada y nueva colocación constituye una
nueva asignación de responsabilidad productiva, no prueba de compra/procedencia.
Se guarda el colocador en SQL antes de resolver la protección. Ante fallo de esa API queda
owner durable/farm pendiente/productividad cero para ese bloque y reintento periódico.
Las asignaciones completas existentes y transferencias explícitas no se sobrescriben.
Apilar en un bloque existente exige que el colocador sea su responsable y que el objeto sea
compatible con su declaración; rechaza mezclas antes de consumir ítems. Otro jugador debe
colocar su propio bloque. No se divide automáticamente un stack entre varios propietarios.

Las raíces de protecciones que coinciden en un punto con spawner se unen durablemente en
un componente de granja. Mismo/diferente titular no cambia al propietario del spawner:
actor conocido manda, titular común solo sirve de fallback del legado sin actor.
Se conserva un único presupuesto de 100 por componente, incluyendo zonas fuera de la
intersección, distintos tipos, alturas y jugadores. Alias administrativos existentes se
vinculan con la granja física sin reescribir sus asignaciones. Componentes separados no
se agrupan por tener el mismo dueño. La ventana continua 33x33 y cupos personales siguen.

## Persistencia, rendimiento y reversión
Tabla aditiva `<stock>_production_farm_links_v1`, pares UUID idempotentes y transaccionales.
No edición directa de stock, loot, XP o RPB; no IDs nuevos en plantillas compartidas.
SQL/LP en worker único. Protección: batches de hasta32 puntos/objetivo2ms, sin cargar chunks.
La revisión periódica de metadata conserva asignaciones activas mientras toma snapshots;
no escaneo global por tick. Uniones transitivas no se deshacen por retirar una protección:
se requiere revisión explícita para separar presupuestos y evitar inflación automática.

Rollback: con autorización de lifecycle, restaurar solo JAR .3 respaldado; conservar BD
actual, stock/loot/XP y sidecars. .3 ignora tabla de enlaces y restaura reglas anteriores
(incluido bug de superposición). No restaurar snapshot histórico sobre datos posteriores.

## Validación
Build/test/shadowJar solo personal con ejecutor limitado. Pruebas unitarias cubren raíces
superpuestas, actor conocido/fallback ambiguo, unión transitiva y orden, cupo compartido
fuera de intersección, colocador, rechazo de apilado ajeno, persistencia y rollback SQL.
Se mantiene fix .3 de negativos en Graal. Simulación standalone en GraalVM del servidor:
3354 filas geométricas de snapshot anterior, seis cupos uniformes, cero negativos. No es
simulación de permisos reales ni prueba de gameplay actual. Gameplay pendiente tras reinicio.

## Prueba después del reinicio diario
1. Confirmar versión .4 y /ss production READY con capacidades >=0.
2. Colocar bloque nuevo: debe aparecer en /ss mine del colocador, aun en terreno de otro.
3. Otro usuario no debe poder agregar unidades a ese stack; puede crear su propio bloque.
4. En protección superpuesta, los bloques asignados deben producir dentro del cupo compartido.
5. Confirmar loot tras ciclo con jugador en rango y sin pausa manual, Mending/Eco, huevos<=50,
   venta de loot anterior, retirada/apilado propio y persistencia tras autosave.
No pedir mover terreno ni colocar más unidades para arreglar propiedad. Bloques antiguos
mal atribuidos requieren transferencia explícita o retirada/colocación después de activar .4.
API reciclaje sigue DRAFT; no afirmar autenticidad/procedencia del legado.

Resultado:152tests/0fallos, build personal aprobado. JAR .4 instalado, SHA256
6b314e871359197389793b8b893c19a17ad6413eca432e7b16887971448b79f7.
Backup original JAR .3: /mnt/storage/backups/ai/survival-custom/smartspawner-custom-overlap-fix-20261011T212446Z.
Backup final previo reemplazo candidato intermedio: smartspawner-custom-overlap-fix-20261011T212643Z.
Snapshots3182bloques/18878unidades, integrity ok; config intacta. Activo .3 hasta reinicio diario.
