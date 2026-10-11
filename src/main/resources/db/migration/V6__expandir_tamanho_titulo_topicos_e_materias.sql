-- =========================================================================
-- V6__expandir_tamanho_titulo_topicos_e_materias.sql
-- Expande o limite de caracteres para títulos de tópicos (de 200 para 1000),
-- nomes de matérias (de 100 para 255) e títulos de planos de estudo (de 150 para 255),
-- suportando descrições detalhadas e itens extensos extraídos de editais de concursos.
-- =========================================================================

ALTER TABLE topicos ALTER COLUMN titulo TYPE VARCHAR(1000);
ALTER TABLE materias ALTER COLUMN nome TYPE VARCHAR(255);
ALTER TABLE planos_estudo ALTER COLUMN titulo TYPE VARCHAR(255);
