<!--
MindmapsDB - A Distributed Semantic Database
Copyright (C) 2016  Mindmaps Research Ltd

MindmapsDB is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MindmapsDB is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
-->

<template>
<div class="container-fluid">

    <div class="row">
        <div class="col-xs-12">
            <div class="panel panel-filled" v-if="response">
                <div class="panel-heading">
                    Current Configuration
                </div>
                <div class="panel-body">
                    <div class="table-responsive">
                        <table class="table table-hover table-stripped">
                            <thead>
                                <tr><th>Config Item</th><th>Value</th><tr>
                            <thead>
                            <tbody>
                                <tr><td>Hostname</td><td>{{response['server.host']}}</td></tr>
                                <tr><td>Server Port</td><td>{{response['server.port']}}</td></tr>
                                <tr><td>Threads</td><td>{{response['loader.threads']}}</td></tr>
                                <tr><td>Database config</td><td>{{response['graphdatabase.config']}}</td></tr>
                                <tr><td>Batch config</td><td>{{response['graphdatabase.batch-config']}}</td></tr>
                                <tr><td>Graph Computer config</td><td>{{response['graphdatabase.computer']}}</td></tr>
                                <tr><td>Engine assets directory</td><td>{{response['server.static-file-dir']}}</td></tr>
                                <tr><td>Log File</td><td>{{response['logging.file']}}</td></tr>
                                <tr><td>Logging Level</td><td>{{response['logging.level']}}</td></tr>
                                <tr><td>Background Tasks time lapse</td><td>{{response['backgroundTasks.time-lapse']}}</td></tr>
                                <tr><td>Background Tasks post processing delay</td><td>{{response['backgroundTasks.post-processing-delay']}}</td></tr>
                                <tr><td>Batch size</td><td>{{response['blockingLoader.batch-size']}}</td></tr>
                                <tr><td>Default Graph Name</td><td>{{response['graphdatabase.default-graph-name']}}</td></tr>
                                <tr><td>Repeat Commits</td><td>{{response['loader.repeat-commits']}}</td></tr>
                                <tr><td>HAL builder degree</td><td>{{response['halBuilder.degree']}}</td></tr>
                                <tr><td>Version</td><td>{{response['project.version']}}</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div class="panel panel-filled panel-c-danger" v-else>
                <div class="panel-heading">
                    Could not connect to Grakn
                </div>
                <div class="panel-body">
                    Have you tried turning it off and on again?
                    <pre class="error-pre" v-show="errorMessage">{{errorMessage}}</pre>
                </div>
                <div class="panel-footer">
                    <button @click="retry" class="btn btn-default">Retry Connection<i class="pe-7s-refresh"></i></button>
                </div>
            </div>
        </div>
    </div>

</div>
</template>

<style>
.pe-7s-refresh {
    padding-left: 5px;
    padding-right: 0px;
}
.error-pre {
    margin-top: 10px;
    margin-bottom: 0px;
}
</style>

<script>
import EngineClient from '../js/EngineClient.js';

export default {
    data() {
        return {
            response: undefined,
            errorMessage: undefined,
            engineClient: {}
        };
    },

    created() {
        engineClient = new EngineClient();
    },

    attached() {
        engineClient.getConfig(this.engineStatus);
    },

    methods: {
        showError(msg) {
            this.response = undefined;
            this.errorMessage = msg;
        },

        engineStatus(resp, err) {
            if(resp != null)
                this.response = resp
            else
                this.showError(err);
        },

        retry() {
            engineClient.getConfig(this.engineStatus);
        }
    }

}
</script>
