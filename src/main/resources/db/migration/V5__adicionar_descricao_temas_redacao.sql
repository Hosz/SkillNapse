-- =========================================================================
-- V5__adicionar_descricao_temas_redacao.sql
-- Adiciona a coluna descricao na tabela temas_redacao para contextualizar a proposta.
-- =========================================================================

ALTER TABLE temas_redacao ADD COLUMN IF NOT EXISTS descricao TEXT;
