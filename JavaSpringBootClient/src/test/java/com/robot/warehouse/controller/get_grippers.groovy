contract {
    request {
        method 'GET',
        url '/getGrippers/1'
    }
    response {
        status 200
        body {
            gripperId 1
            status 'idle'
        }
    }
}