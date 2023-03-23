class BackendAPI {

    constructor(route="") {
        this.route = route;
    }

    getStartup(){
        return new Promise((resolve, reject) => {
            const req = new Request(`${this.route}/startup`);
            const init = {
                method: "GET"
            }
            fetch(req, init).then(response => {
                response.text().then(textResponse => {
                    resolve(textResponse);
                }).catch(reason => {
                    reject(reason);
                })
            }).catch(reason => {
                reject(reason);
            })
        })
    }

    getOutputs(){
        return new Promise((resolve, reject) => {
            const req = new Request(`${this.route}/outputs`);
            const init = {
                method: "GET"
            }
            fetch(req, init).then(response => {
                response.json().then(jsonResponse => {
                    resolve(jsonResponse);
                }).catch(reason => {
                    reject(reason);
                })
            }).catch(reason => {
                reject(reason);
            })
        })
    }

    execute(command){
        return new Promise((resolve, reject) => {
            const req = new Request(`${this.route}/exec`);
            const init = {
                method: "POST",
                body: command
            }
            fetch(req, init).then(response => {
                response.json().then(jsonResponse => {
                    resolve(jsonResponse.response);
                }).catch(err => reject(err));
            }).catch(reason => {
                reject(reason);
            })
        })
    }

    startOutput(output){
        return new Promise((resolve, reject) => {
            const req = new Request(`${this.route}/startOutput`);
            const init = {
                method: "POST",
                body: JSON.stringify(output)
            }
            fetch(req, init).then(response => {
                response.json().then(jsonResponse => {
                    resolve(jsonResponse);
                }).catch(reason => {
                    reject(reason);
                })
            }).catch(reason => {
                reject(reason);
            })
        })
    }

}

export default BackendAPI;