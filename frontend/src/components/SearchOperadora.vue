<template>
  <div class="container">
    <h2>Busca de Operadoras de Saúde</h2>
    
    <div class="search-box">
      <input
        v-model="busca"
        @keyup.enter="buscar"
        placeholder="Digite o nome (mínimo 2 caracteres)"
      />
      
      <select v-model="ufFiltro" class="uf-select">
        <option value="">Todos estados</option>
        <option v-for="estado in estadosBR" :value="estado.sigla" :key="estado.sigla">
          {{ estado.nome }}
        </option>
      </select>
      
      <button @click="buscar" :disabled="busca.length < 3 || carregando">
        {{ carregando ? 'Buscando...' : 'Buscar' }}
      </button>
    </div>

    <div v-if="erro" class="error-message">
      {{ erro }}
    </div>

    <div v-if="carregando" class="loading">
      Carregando resultados...
    </div>

    <div v-else>
      <div v-if="resultados.length" class="results-summary">
        {{ resultados.length }} resultados encontrados para "{{ busca }}"
        <span v-if="ufFiltro">no {{ ufFiltro }}</span>
      </div>
      
      <ul class="results-list">
        <li v-for="operadora in resultados" :key="operadora.registro_ans" class="result-item">
          <h3>{{ operadora.nome_fantasia }}</h3>
          <p><strong>Registro ANS:</strong> {{ operadora.registro_ans }}</p>
          <p><strong>Razão Social:</strong> {{ operadora.razao_social }}</p>
          <p><strong>Modalidade:</strong> {{ operadora.modalidade }}</p>
          <p><strong>UF:</strong> {{ operadora.uf }}</p>
          <p v-if="operadora.municipio"><strong>Município:</strong> {{ operadora.municipio }}</p>
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'SearchOperadora',
  data() {
    return {
      busca: '',
      ufFiltro: '',
      resultados: [],
      carregando: false,
      erro: null,
      estadosBR: [
        { sigla: 'AC', nome: 'Acre' },
        { sigla: 'AL', nome: 'Alagoas' },
        // ... todos os estados ...
        { sigla: 'SP', nome: 'São Paulo' },
        { sigla: 'TO', nome: 'Tocantins' }
      ]
    };
  },
  methods: {
    async buscar() {
      if (this.busca.length < 3) {
        this.erro = 'Digite pelo menos 2 caracteres';
        return;
      }

      this.carregando = true;
      this.erro = null;
      this.resultados = [];

      try {
        const params = {
          termo: this.busca
        };

        if (this.ufFiltro) {
          params.uf = this.ufFiltro;
        }

        const response = await axios.get('http://localhost:8000/buscar', {
          params,
          timeout: 10000
        });

        if (response.data.sucesso) {
          this.resultados = response.data.resultados;
          if (this.resultados.length === 0) {
            this.erro = `Nenhum resultado encontrado para "${this.busca}"` + 
              (this.ufFiltro ? ` no ${this.ufFiltro}` : '');
          }
        } else {
          this.erro = 'Nenhum resultado encontrado';
        }

      } catch (error) {
        console.error('Erro completo:', error);
        
        if (error.response) {
          // Erro 4xx/5xx do servidor
          this.erro = `Erro no servidor: ${error.response.status}`;
        } else if (error.request) {
          // Servidor não respondeu
          this.erro = 'Servidor não está respondendo. Verifique se o backend está rodando.';
        } else {
          // Erro na configuração
          this.erro = 'Erro ao configurar a requisição';
        }
        
      } finally {
        this.carregando = false;
      }
    }
  }
};
</script>

<style scoped>
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Arial', sans-serif;
}

.search-box {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.uf-select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 150px;
}

button {
  padding: 10px 20px;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

button:hover:not(:disabled) {
  background-color: #369f6b;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.error-message {
  color: #e74c3c;
  padding: 10px;
  background-color: #fdeded;
  border-radius: 4px;
  margin-bottom: 15px;
}

.loading {
  padding: 20px;
  text-align: center;
  color: #666;
}

.results-summary {
  margin: 15px 0;
  font-size: 14px;
  color: #666;
}

.results-list {
  list-style: none;
  padding: 0;
}

.result-item {
  background-color: #f9f9f9;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 10px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.result-item h3 {
  margin-top: 0;
  color: #2c3e50;
}

.result-item p {
  margin: 5px 0;
  color: #555;
}

@media (max-width: 600px) {
  .search-box {
    flex-direction: column;
  }
  
  .uf-select {
    width: auto;
  }
}
</style>