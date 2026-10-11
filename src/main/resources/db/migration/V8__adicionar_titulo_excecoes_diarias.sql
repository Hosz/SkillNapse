-- V8: Adicionar campo titulo para excecoes diarias (BLOCO_AVULSO e SUBSTITUIR_HORARIO)
ALTER TABLE excecoes_diarias
    ADD COLUMN IF NOT EXISTS titulo VARCHAR(255);
