-- =========================================================================
-- V7__adicionar_titulo_blocos_horario_template.sql
-- Adiciona a coluna 'titulo' na tabela 'blocos_horario_template' para permitir
-- nomes e descrições personalizadas para cada bloco de estudo no cronograma.
-- =========================================================================

ALTER TABLE blocos_horario_template
    ADD COLUMN titulo VARCHAR(255);

-- Preenche blocos existentes com o título do tópico ou nome da matéria associada
UPDATE blocos_horario_template b
SET titulo = COALESCE(t.titulo, m.nome, 'Bloco de Estudo')
FROM blocos_horario_template b2
LEFT JOIN topicos t ON b2.topico_id = t.id
LEFT JOIN materias m ON b2.materia_id = m.id
WHERE b.id = b2.id AND b.titulo IS NULL;
