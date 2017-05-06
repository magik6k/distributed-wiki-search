'use strict'

const path = require('path');
module.exports = require('./scalajs.webpack.config');

module.exports.resolve = {
    alias: {
        zlib: 'browserify-zlib-next'
    }
};

//Fix npm link
module.exports.resolve = { fallback: path.join(__dirname, "node_modules") };
module.exports.resolveLoader = { fallback: path.join(__dirname, "node_modules") };

module.exports.module.preLoaders.push({ test: /\.json$/, loader: 'json-loader' });
