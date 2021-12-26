Awesome Gateway Apache Dubbo plugin sample
---

dubbo 新增路由案例
----

```
curl --location --request POST 'localhost:9876/route/put' \
--header 'Content-Type: application/json' \
--data-raw '{
    "routes": [
        {
            "id": "5",
            "service": "dubbo://dubbo",
            "gatewayPath": "/testItem",
            "protocol": "dubbo",
            "metadata": {
                "registryAddress": "nacos://127.0.0.1:8848",
                "interfaceName": "com.ewell.sample.dubbo.api.ItemInterface",
                "methodName": "getItemList",
                "version": "1.0.0",
                "paramsMeta": [
                    {
                        "paramName": "commodityId",
                        "paramType": "java.util.List",
                        "sort": 1
                    },
                    {
                        "paramName": "appId",
                        "paramType": "java.lang.Integer",
                        "sort": 2
                    }
                ]
            }
        }
    ]
}'
```

dubbo 路由案例
----

```
curl --location --request POST 'http://127.0.0.1:8099/testItem' \
--header 'Content-Type: text/plain' \
--data-raw '{
    "commodityId":["11212"],
    "appId":12121
}'
```

