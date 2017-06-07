urdf.add("http://amaa.me/light/light", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://uri.etsi.org/m2m/saref#LightSwitch")
urdf.add("http://amaa.me/light/light", "http://www.w3.org/ns/td/hasAction", "http://amaa.me/light/lightonoff")
urdf.add("http://amaa.me/light/room1", "http://www.w3.org/2003/01/geo/wgs84_pos#longitude", "42.00")
urdf.add("http://amaa.me/light/room1", "http://www.w3.org/ns/td/hasProperty", "http://amaa.me/light/temp")
urdf.add("http://amaa.me/light/room1", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/ns/sosa/FeatureOfInterest")
urdf.add("http://amaa.me/light/lightonoff", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/ns/td/Action")
urdf.add("http://amaa.me/light/lightonoff", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/ns/sosa/Procedure")
urdf.add("http://amaa.me/light/room1", "http://www.w3.org/2003/01/geo/wgs84_pos#latitude", "42.00")
urdf.add("http://amaa.me/light/light", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/ns/td/Thing")
urdf.add("http://amaa.me/light/lightonoff", "http://www.w3.org/ns/td/hasLink", "coap://example.org/light/onoff")
urdf.add("http://amaa.me/light/room1", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://uri.etsi.org/m2m/saref#BuildingSpace")
urdf.add("http://amaa.me/light/room1", "http://uri.etsi.org/m2m/saref#contains", "http://amaa.me/light/light")
urdf.add("http://amaa.me/light/lightonoff", "http://www.w3.org/ns/ssn/implementedBy", "http://amaa.me/light/light")

c = mqtt.Client('urdf-nodemcu', 120)

function null(c) end
topic = 'urdf/amaa/'

c:on('message', function(c, t, d)
    local errn, res = urdf.query(d)
    if errn == 0 then
        c:publish(topic..'urdf', res, 0, 0, null)
    else
        c:publish(topic..'failure', 'hem... ('..errn..')', 0, 0, null)
    end
end)

c:connect('192.168.4.2', null, null)
c:subscribe(topic..'usparql', 0, null)
