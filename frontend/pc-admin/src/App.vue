<template>
  <n-message-provider>
    <router-view />
  </n-message-provider>
</template>

<script setup lang="ts">
import { NMessageProvider } from 'naive-ui'
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
}
</style>
