# Kafka Connect

## [Exemplo 1](https://github.com/sammubr/kafka-connect/tree/main/example1)

Sincroniza bases de dados mysql na origem e mysql no destino, usando JDBC Sink Connector em ambos:

- Ao inserir um registro no database test1, o mesmo será inserido automaticamente no database teste2

- Ao atualizar um registro no database test1, é necessário colocar a data atual no campo last_modified_date, para que o mesmo seja atualizado no database test2

- Não foi configurado exclusão nessa sincronização

## [Exemplo 2](https://github.com/sammubr/kafka-connect/tree/main/example2)

Sincroniza bases de dados mysql na origem e mysql no destino, usando Debezium MySQL Connector e JDBC Sink Connector:

- Ao inserir um registro no database test1, o mesmo será inserido automaticamente no database teste2

- Ao atualizar um registro no database test1, é necessário colocar a data atual no campo last_modified_date, para que o mesmo seja atualizado no database test2

- Ao excluir um registro no database test1, o mesmo será excluído automaticamente no database teste2

## [Exemplo 3](https://github.com/sammubr/kafka-connect/tree/main/example3)

Sincroniza bases de dados mongo na origem e mongo no destino, usando Mongo Kafka Connector:

- Ao inserir um registro no database test1, o mesmo será inserido automaticamente no database teste2

- Ao atualizar um registro no database test1, o mesmo será atualizado automaticamente no database teste2

- Ao excluir um registro no database test1, o mesmo será excluído automaticamente no database teste2
