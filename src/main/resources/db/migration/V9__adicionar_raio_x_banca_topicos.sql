-- =========================================================================
-- V9__adicionar_raio_x_banca_topicos.sql
-- Adiciona suporte ao Raio-X da Banca e justificativa de peso pedagógico.
-- =========================================================================

ALTER TABLE planos_estudo ADD COLUMN banca VARCHAR(100);

ALTER TABLE topicos ADD COLUMN relevancia_banca VARCHAR(30);
ALTER TABLE topicos ADD COLUMN justificativa_peso VARCHAR(1000);
