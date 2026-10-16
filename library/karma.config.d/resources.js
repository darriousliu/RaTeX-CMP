// Serve the same Compose resources shipped with the library in both browser test targets.
config.files.push({
    pattern: config.basePath + '/kotlin/composeResources/**/*',
    included: false,
    served: true,
    watched: false
});
config.proxies['/composeResources/'] = '/base/kotlin/composeResources/';
