



export class Client {

    static async login(username, password) {
        const response = await fetch("http://127.0.0.1:9000/signin", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                username,
                password
            }),
        });
        const data = await response.json();
        return data;
    }


    static async updateAccessToken(refreshToken) {
        const response = await fetch("http://127.0.0.1:9000/token/refresh", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                refreshToken
            }),
        });
        const data = await response.json();
        return data;
    }

    // static async user() {
    //     // const response = await 
    //    return securedFetch("http://127.0.0.1:9000/user")
    //     .then(data => data.json())
    //     // const data = await response.json();
    //     // return data;
    // }

}





export default Client