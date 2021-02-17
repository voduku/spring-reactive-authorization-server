# Spring Reactive Authorization Server

## GOAL

Support projects with older version of spring looking to convert to reactive and/or waiting for `Spring Authorization Server` project to be released with
reactive implementation.

## NOTES

- I have no intention have claiming Pivotal works as my own. I only create a reactive version of old spring authorization server.
- I am still a young developer with 2 years of experience so if you read through my implementation and found any bad coding style or bad implementation. Please
  kindly contact me, create an issue or leave a PR.
- Currently, this implementation only support up to version 2.3.8 of spring-security-oauth2. I decided not to go for later versions because most features are
  deprecated.
- Since there are many endpoints supported in the old version, I only had time to implement tokenEndpoint which expose `/oauth/token`. If you use other
  endpoints, please create a reactive implementation of that endpoint and submit a PR

## CONTRIBUTIONS

- You are welcome to refactor and submit a PR since I haven't split the reactive implementation to a separate module.
- Any other fix needs to be reviewed carefully by PR owner and hopefully someone that understand this better than me.
- I don't have the resource to add put this project on online so any help with this would help.

## WHAT TO KNOW

- The implementations for reactive Authorization Server is in `./src/main/java/com/example/uaa/authorization`
- All you needs to do in order to enable it is to add `@EnableReactiveAuthorizationServer`
- For the time being, I only support opaque token and info about that token will be saved in a database. You are welcome to add any other implementations that
  you need. Make sure you register your implementation!
- If you want to implement other endpoints such as `/oauth/authorize` all you need to do is create that endpoint and add `@FrameworkReactiveEndpoint`
- If you want to try the sample, make sure you change the database info in `./src/main/resources/application.yml`