-- =========================================================================
-- V9__expandir_tamanho_titulo_simulados.sql
-- Expande o limite de caracteres para títulos de simulados (de 150 para 300),
-- acomodando títulos hierárquicos detalhados (ex: Matéria > Tópico > Subtópico + Banca).
-- =========================================================================

ALTER TABLE simulados ALTER COLUMN titulo TYPE VARCHAR(300);
