# D7 — cupos por tipo

Corrección solicitada por el propietario: granja y geometría espacial se cuentan POR TIPO,
reemplazando la suma de todos los tipos de D3–D6. Esta es la política vigente.
Personal: default50/guardian60/ascendido70/arcano80/elite90/leyenda100 por responsable/tipo,
mediante permisos efectivos. Granja:100 por componente durable/tipo, compartidos entre
jugadores y todas las alturas. Espacial:100 por tipo en cualquier ventana horizontal33x33,
entre todas las granjas/jugadores/alturas. MOB:EntityType e ITEM:Material no mezclan presupuestos;
variantes/aliases del mismo tipo no crean cupos extras. No nuevo límite global por usuario.

Ejemplo: seis stacks de50 (dos Blaze, dos cave spiders, dos iron golems), responsables con
cupo100 y sin consumo adicional del mismo tipo:300 productivos,100porcadauno, en una granja
compacta. Añadir otra cuenta no eleva el máximo compartido del mismo tipo.

Cambio aislado en ProductionCapacityPlanner: claves FarmType(farm,type) y WorldType(world,type).
Mantiene orden por ID, ventanas continuas, guard de capacidad no negativa/fixGraal. No cambia
propiedad, alias durable de granja, stock real inclunloaded, ventas, drops,XP,callbacks ni huevo50.
Contiene D6: colocador directo, superposiciones y rechazo de apilado ajeno. ConfigCustom sigue
max_stack_size50 (reload previo autorizado); no permite activar este cambio con /ss reload.

Migración: ninguna edición de datos anteriores ni cambio de schema. El plan se recalcula con
las mismas unidades y responsables al próximo arranque. Asignaciones antiguas erróneas no
se reatribuyen; transferir expresamente o retirar/colocar después de activar el colocador nuevo.
No alterar stop manual ni inferir producción de un stack en pausa/fuera de rango.

Validación automatizada: seis rangos/tres tipos en un mismo sitio, seis stacks y cuenta amiga,
separación item/mob, cap del mismo tipo entre granjas, bordes/esquinas/chunks vecinos y
ventanas exhaustivas aleatorias ahora agrupadas por tipo. Se conservan regresiones D6 yGraal.
Standalone GraalVM usa snapshot geométrico3354filas y seis rangos uniformes; no son permisos
reales ni benchmark de gameplay. Gameplay pendiente después del reinicio diario.

Entrega solo Custom: JAR en disco para reinicio diario del propietario; sin restart/reload.
Backup HDD incluye JAR previo .4, config y SQLite consistente. Si hay rollback, restaurar
solo JAR respaldado con autorización de lifecycle; conservar datos/loot/XP posteriores.
Rollback .4 restaura suma de todos los tipos; referencia .3 original en D6 si se necesita.

Comprobar tras reinicio: versión .5 y /ss production READY/capacidad>=0; cada tipo debería
recibir su cupo independiente, /ss mine debe indicar al colocador en bloques nuevos. Verificar
loot real en rango, sin pausa, apilado50/huevo50/MendingEco y propiedad. No confundir un cupo
personal100 con100porcada bloque o un límite de stock. APIreciclaje permanece DRAFT.

Resultado:155tests/0fallos, shadowJar personal aprobado. .5 instalada Custom SHA256
0c191d7eef15dd137f867834d061540644e25b307a17c46fce4cb128f110e91f.
Backup /mnt/storage/backups/ai/survival-custom/smartspawner-custom-per-type-20261011T215717Z.
SQLite snapshot consistente/integrityok:3194bloques/19305unidades; config intacta.
GraalVM standalone3354filas:13547/14159/14548/14842/15085/15315 unidades productivas con
cupos uniformes50/60/70/80/90/100 respectivamente; cero negativos, planificación33–108ms.
Esto es snapshot anterior y rangos uniformes, no capacidad actual de Custom ni su TPS.
Activo .3 al instalar; .5 pendiente del reinicio diario. No se ejecutó lifecycle.
