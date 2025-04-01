CREATE DATABASE nivelamento_db;

use nivelamento_db;


CREATE TABLE demonstracoes_contabeis (
    DATA DATE,
    Reg_Ans VARCHAR(255),
    Cd_Conta_Contabil VARCHAR(100),
    Descricao TEXT,
    Vl_Saldo_Inicial DECIMAL(15,2),
    Vl_Saldo_Final DECIMAL(15,2)
);



LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\1T2023.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\2T2023.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\3T2023.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\4T2023.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\1T2024.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\2T2024.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\3T2024.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Dados_Csv\\4T2024.csv'
INTO TABLE demonstracoes_contabeis
FIELDS TERMINATED BY ';'
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;


SELECT 
    Reg_Ans AS Operadora, 
    SUM(Vl_Saldo_Final - Vl_Saldo_Inicial) AS despesa_trimestral
FROM demonstracoes_contabeis
WHERE (Descricao) = 'EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS  DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR '
AND DATA >= (SELECT MAX(DATA) FROM demonstracoes_contabeis) - INTERVAL 3 MONTH
GROUP BY Reg_Ans
ORDER BY despesa_trimestral DESC
LIMIT 10;


SELECT 
    Reg_Ans AS Operadora, 
    SUM(Vl_Saldo_Final - Vl_Saldo_Inicial) AS despesa_Anual
FROM demonstracoes_contabeis
WHERE (Descricao) = 'EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS  DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR '
AND DATA >= (SELECT MAX(DATA) FROM demonstracoes_contabeis) - INTERVAL 1 YEAR
GROUP BY Reg_Ans
ORDER BY despesa_Anual DESC
LIMIT 10;



CREATE TABLE operadoras_plano_saude(
    Registro_ANS VARCHAR(255) PRIMARY KEY,
    CNPJ VARCHAR(20),
    Razao_Social VARCHAR(255),
    Nome_Fantasia VARCHAR(255),
    Modalidade VARCHAR(255),
    Logradouro TEXT,
    Numero VARCHAR(50),
    Complemento TEXT,
    Bairro VARCHAR(100),
    Cidade VARCHAR(100),
    UF VARCHAR(2),
    CEP VARCHAR(10),
    DDD VARCHAR(3),
    Telefone VARCHAR(20),
    Fax VARCHAR(20),
    Endereco_eletronico TEXT,
    Representante TEXT,
    Cargo_Representante TEXT,
    Regiao_de_Comercializacao INT,
    Data_Registro_ANS DATE
);


TRUNCATE TABLE operadoras_plano_saude;

LOAD DATA LOCAL INFILE 'C:\\Users\\bryan\\Documents\\IntuitiveCareNivelamento\\Relatorio_cadop.csv'
INTO TABLE operadoras_plano_saude
CHARACTER SET utf8mb4  -- Teste com UTF-8
FIELDS TERMINATED BY ';'
OPTIONALLY ENCLOSED BY '"'
ESCAPED BY '\\'  -- Adiciona tratamento de escape
LINES TERMINATED BY '\n'  -- Teste com apenas \n
IGNORE 1 LINES;


SHOW DATABASES;

