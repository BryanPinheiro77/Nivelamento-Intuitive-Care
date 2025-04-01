<template>
  <div id="app">
    <h1>Buscar Operadora</h1>
    <SearchOperadora />
  </div>
</template>

<script>
import SearchOperadora from './components/SearchOperadora.vue'; // Verifique o nome aqui!

export default {
  components: {
    SearchOperadora // Certifique-se de usar o nome correto aqui!
  }
};
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
