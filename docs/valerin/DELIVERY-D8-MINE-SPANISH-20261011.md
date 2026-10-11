# D8 — /ss mine en español con filtros

Versión1.8.3-valerin-production.6, instalada en Custom para reinicio diario del propietario.
Sin restart/reload; activo .3 al entregar. Incluye D6 colocador/superposiciones y D7 cupos por tipo.
Stack máximo50 y huevo50 siguen como están; no cambió config ni datos de stock/loot/XP.

Título/resumen del menú: «300 colocados · 200 produciendo», números según el filtro actual.
Cada entrada muestra criatura traducida vía formatting.mob_names, cantidad, produciendo,
unidades sin producir, responsable «Tú», mundo y ubicación. Solo responsable UUID del usuario,
incluye mundos/chunks descargados; no último interactor ni dueño de protección.

Controles estilo /ss list: anterior45, filtro criatura46, estado48, cerrar49, orden50,
resumen51, siguiente53. Filtros Todos/Produciendo/Con unidades sin producir; tipos disponibles
solo del usuario. Orden tipo/más colocados/más produciendo. Cambiar filtros vuelve a página1,
conservar filtro al navegar. Stack parcial aparece tanto en Produciendo como en Con unidades
sin producir. Totales corresponden al filtro completo, no solo página actual45 entradas.
Sonido UI_BUTTON_CLICK igual a list, verde37eb9a/azul5DB5F1/advertenciaF0C857/cerrarFF6B6B.
Lista privada/read-only: no gestiona otros ni abre acciones administrativas/carga chunks.

«Produciendo» es nombre solicitado para capacidad habilitada. No acredita generación instantánea:
lore del resumen explica jugador cercano y no pausado. No inventa motivo individual de capacidad0.
Spawners en pausa siguen su estado; cálculo de cupo de producción no fue modificado en D8.

Textos traducidos de mine/production y avisos de mezcla de dueño, máximo de stack y huevo.
Alertas al consultar/actuar, sin mensajes periódicos nuevos. Nuevas claves presentes en todos
los locales; español en es_ES y layouts Donut usados por Valerin. Custom activo en_US_DonutSMP_v2:
se editaron solo claves de esta entrega en3 archivos (command_gui,command_messages,messages),
preservando semánticamente resto de /ss list y otras mecánicas. Actualizar solo el JAR no habría
traducido los campos ya existentes, porque LanguageUpdater conserva personalizaciones.

SQL/LP siguen en worker; GUI usa callback de entidad. Peticiones con tokens/inventario esperado
impiden que respuesta vieja reabra menú cerrado/reemplazado. Cancela click/drag, limpia peticiones
al cerrar/desconectar. Una nueva consulta supersede la anterior para ese usuario.

Pruebas:157unitarias/0fallos y shadowJar personal limitado, YAML/claves de todos locales verificados.
Regresiones nuevas: privacidad en filtros/tipos/totales, stacks parciales, orden y página vacía.
Gameplay visual/sonidos/navegación tras reinicio diario pendiente; no se ejecutó servidor aparte.

SHA256605ea7afe42608f4c1c803be379e11889294fca6c19d5c42a25fd7dfeafee7e7.
Backup HDD /mnt/storage/backups/ai/survival-custom/smartspawner-custom-mine-ui-20261011T220438Z:
JAR previo .5, config/idiomas y SQLite consistente/integrityok3171bloques/18816unidades.
Config hash no cambió. DEVELOPMENT_HANDOFF.md ajeno permanece intacto/untracked.

Rollback: restaurar JAR .5 y solo los3 archivos de idioma respaldados con autorización de lifecycle;
conservar BD y loot/XP posteriores. No restaurar SQLite histórico por un rollback de interfaz.
Comprobar después de reinicio:version.6; /ss mine propio y vacío; filtros mob/estado/orden/paginación;
sonidos, acentos/colores, spam, cambio rápido de filtro/cierre mientras consulta; casos permiso
quitado/desconexión; recordar que asignaciones viejas no se reatribuyen automáticamente.
