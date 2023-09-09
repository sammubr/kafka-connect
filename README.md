# Kafka Connect

## Exemplo 1

Sincroniza bases de dados mysql na origem e mysql no destino, usando JDBC Sink Connector em ambos:

- Ao inserir um registro no database test1, o mesmo será inserido automaticamente no database teste2

- Ao atualizar um registro no database test1, é necessário colocar a data atual no campo last_modified_date, para que o mesmo seja atualizado no database test2

- Não foi configurado exclusão nessa sincronização

## Exemplo 2

Sincroniza bases de dados mysql na origem e mysql no destino, usando Debezium MySQL Connector e JDBC Sink Connector:

- Ao inserir um registro no database test1, o mesmo será inserido automaticamente no database teste2

- Ao atualizar um registro no database test1, é necessário colocar a data atual no campo last_modified_date, para que o mesmo seja atualizado no database test2

- Ao excluir um registro no database test1, o mesmo será excluído automaticamente no database teste2