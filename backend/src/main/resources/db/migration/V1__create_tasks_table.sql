CREATE TABLE tasks (
    id          bigserial      PRIMARY KEY,
    title       varchar(120)   NOT NULL,
    description varchar(2000),
    status      varchar(20)    NOT NULL,
    priority    varchar(20)    NOT NULL,
    due_date    date,
    created_at  timestamptz    NOT NULL,
    updated_at  timestamptz    NOT NULL
);

-- Filtro da listagem por status (feature 004). A busca por título continua sem
-- índice: é LIKE com curinga à esquerda, que um índice B-tree não atende.
CREATE INDEX idx_tasks_status ON tasks (status);
