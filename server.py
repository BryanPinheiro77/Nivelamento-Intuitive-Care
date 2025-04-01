from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError
from typing import Optional
import logging
import unicodedata

# Configuração de logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

# Configuração do CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Conexão com o banco de dados
DATABASE_URL = "mysql+pymysql://root:19030602@localhost/nivelamento_db"
engine = create_engine(DATABASE_URL)

def normalizar_texto(texto: str) -> str:
    """Remove acentos e converte para minúsculas"""
    if not texto:
        return ""
    texto = unicodedata.normalize('NFD', texto)
    texto = texto.encode('ascii', 'ignore').decode('utf-8')
    return texto.lower()

@app.get("/buscar")
async def buscar_operadoras(
    termo: str = Query(..., min_length=2),
    uf: Optional[str] = Query(None, min_length=2, max_length=2)
):
    try:
        termo_busca = normalizar_texto(termo)
        logger.info(f"Buscando: {termo_busca} | UF: {uf}")

        # Query com a estrutura real da tabela
        query = text("""
            SELECT 
                Registro_ANS AS registro_ans,
                CNPJ AS cnpj,
                Razao_Social AS razao_social,
                Nome_Fantasia AS nome_fantasia,
                Modalidade AS modalidade,
                UF AS uf,
                Cidade AS cidade
            FROM operadoras_plano_saude
            WHERE (LOWER(Nome_Fantasia) LIKE :termo
               OR LOWER(Razao_Social) LIKE :termo)
        """)
        
        params = {"termo": f"%{termo_busca}%"}
        
        if uf:
            query = text("""
                SELECT 
                    Registro_ANS AS registro_ans,
                    CNPJ AS cnpj,
                    Razao_Social AS razao_social,
                    Nome_Fantasia AS nome_fantasia,
                    Modalidade AS modalidade,
                    UF AS uf,
                    Cidade AS cidade,
                    CASE
                        WHEN LOWER(Nome_Fantasia) LIKE :termo_exato THEN 2
                        WHEN LOWER(Razao_Social) LIKE :termo_exato THEN 1
                        ELSE 0
                    END AS relevancia
                FROM operadoras_plano_saude
                WHERE (LOWER(Nome_Fantasia) LIKE :termo_parcial 
                      OR LOWER(Razao_Social) LIKE :termo_parcial)
                      AND UF = :uf
                ORDER BY relevancia DESC, Nome_Fantasia ASC
            """)
            params = {
                "termo_parcial": f"%{termo_busca}%",
                "termo_exato": f"{termo_busca}%",
                "uf": uf.upper()
            }

        with engine.connect() as conn:
            result = conn.execute(query, params)
            operadoras = [dict(row._asdict()) for row in result]
            logger.info(f"Resultados encontrados: {len(operadoras)}")

        return {
            "sucesso": True,
            "termo": termo,
            "quantidade": len(operadoras),
            "resultados": operadoras
        }

    except SQLAlchemyError as e:
        logger.error(f"Erro no banco: {str(e)}")
        raise HTTPException(status_code=500, detail="Erro no banco de dados")
    except Exception as e:
        logger.error(f"Erro inesperado: {str(e)}")
        raise HTTPException(status_code=500, detail="Erro interno no servidor")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=True)