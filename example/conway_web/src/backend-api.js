class BackendAPI {

    ensureConwayDefault() {
        return new Promise((resolve, reject) => {
            const req = new Request(`/exec`);
            const init = {
                method: "POST",
                body: `default Conway`
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

    newGeneration(game){
        return new Promise((resolve, reject) => {
            this.ensureConwayDefault().then(() => {
                const req = new Request(`/exec`);
                const init = {
                    method: "POST",
                    body: `p ${game}`
                }
                fetch(req, init).then(response => {
                    response.json().then(jsonResponse => {
                        resolve(jsonResponse.response);
                    }).catch(err => reject(err));
                }).catch(reason => {
                    reject(reason);
                })
            }).catch(reason => {
                reject(reason);
            })
        })
    }

    nextGeneration(configuration){
        return new Promise((resolve, reject) => {
            this.ensureConwayDefault().then(() => {
                const req = new Request(`/exec`);
                const init = {
                    method: "POST",
                    body: `p generation(${configuration.generationString})`
                }
                fetch(req, init).then(response => {
                    response.json().then(jsonResponse => {
                        resolve(jsonResponse.response);
                    }).catch(err => reject(err));
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