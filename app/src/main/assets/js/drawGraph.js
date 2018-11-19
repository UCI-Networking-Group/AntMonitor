var svg, force, node, link, nodes, links;

function initGraph(pHeight, pWidth) {
    //console.log("initGraph")

    var height = parseInt(pHeight);
    var width = parseInt(pWidth);

    force = d3.layout.force()
        .size([width, height])
        .gravity(0.05)
        .linkDistance(100)
        .charge(-500)

    force.on("tick", tick);

    nodes = force.nodes();
    links = force.links();

    svg = d3.select("#id_svg_graph")
            .attr("height",height)
            .attr("width",width);

    svg.append("rect")
       .attr("height", height)
       .attr("width", width);

    //For making the graph directed
    svg.append("defs").selectAll("marker")
    .data(["suit","licensing","resolved"])
    .enter().append("marker")
    .attr("id",function(d){return d;})
    .attr("viewBox","0 -5 10 10")
    .attr("refX",25)
    .attr("refY",0)
    .attr("markerWidth",6)
    .attr("markerHeight",6)
    .attr("orient","auto")
    .append("path")
    .attr("d","M0,-5 L10,0L0,5 L10,0 L0,-5")
    .style("stroke","#4679BD")
    .style("opacity","0.6");


    update();
}

function tick() {
    link.attr("x1", function(d) {return d.source.x;})
        .attr("y1", function(d) {return d.source.y;})
        .attr("x2", function(d) {return d.target.x;})
        .attr("y2", function(d) {return d.target.y;});

    node.attr("transform", function(d) {return "translate(" + d.x + "," + d.y + ")";});
}

function update() {
    //console.log("UPDATE");
    link = svg.selectAll("line.link")
        .data(links);

    link.enter().insert("line")
        .attr("class", "link")
        .attr("id", function(d) { return d.source.id + "-" + d.target.id; })
        .attr("byteCount", function(d) { return d.byteCount; })
        .style("marker-end","url(#suit)");
        //.on("click", function(d) { window.alert(""+ d.byteCount + " bytes"); });

    link.exit().remove();

    node = svg.selectAll("g.node")
        .data(nodes, function(d) {return d.id; });

    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .call(force.drag);

    nodeEnter.append("image")
        .attr("xlink:href", function(d) { return "data:image/png;base64,"+d.image})
        .attr("x", -32)
        .attr("y", -32)
        .attr("width", 64)
        .attr("height", 64)
        .on("click", function(d) {
            if (d.isApp) {
                JavaInterface.showAppInfo(d.id.replace(/_/g, '.').substring(1));
            } else {
                JavaInterface.showServerInfo(d.id.replace(/_/g, '.').substring(1));
            }
        });

    node.exit().remove();

    // Restart the force layout.
    force
      .nodes(nodes)
      .links(links)
      .start();
}

function addNode(id, image) {
    nodes.push({"id": id, "image" : image, "isApp": true});
}

function addServerNode(id, image) {
    nodes.push({"id": id, "image" : image, "isApp": false});
}


function findNode(id) {
    for (var i = 0; i< nodes.length; i++) {
        if (nodes[i].id === id) {
            return nodes[i];
        }
    }
}

function findNodeIndex(id) {
    for (var i = 0; i<nodes.length; i++) {
        if(nodes[i].id === id) {
            return i;
        }
    }
}

function addLink (sourceId, targetId, length) {
    var sourceNode = findNode(sourceId);
    var targetNode = findNode(targetId);
    var byteCount = parseInt(length);

    var linkAlreadyExist = false, i =0;
    while (i < links.length) {
        if ((links[i].source == sourceNode) && (links[i].target == targetNode)) {
            links[i].byteCount = parseInt(links[i].byteCount) + byteCount;
            return;
        }
        else
            i ++;
    }
    if ((sourceNode !== undefined) && (targetNode !== undefined)) {
        links.push({"source": sourceNode, "target": targetNode, "byteCount": byteCount});
    }

}

function removeConnection (sourceId, targetId) {
    sourceId = "_" + sourceId.replace(/\./g,"_");
    targetId = "_" + targetId.replace(/\./g,"_");

    var sourceNode = findNode(sourceId);
    var targetNode = findNode(targetId);

    var i = 0;

    var linkExists = false;
    while (i<links.length) {
        if ((links[i].source === sourceNode) && (links[i].target === targetNode)) {
            links.splice(i,1);
            linkExists = true;
            break;
        } else {
            i++;
        }
    }

    i = 0;
    while (i < links.length) {
        if ((links[i].source === targetNode) && (links[i].target === sourceNode)) {
            links.splice(i,1);
            linkExists = true;
            break;
        } else {
            i++;
        }
    }
    if (linkExists) {
        var isAppName = false;
        var isIpName = false;

        i =0;
        while (i < links.length) {
            if (links[i].source === sourceNode) {
                isAppName = true;
                break;
            } else {
                i++;
            }
        }

        i = 0;
        while (i < links.length) {
            if (links[i].target === targetNode) {
                isIpName = true;
                break;
            } else {
                i++;
            }
        }

        if (!isAppName) {
            var AppIndex = findNodeIndex(sourceId);
            nodes.splice(AppIndex, 1);
            //update();
        }

        if (!isIpName) {
            var IpIndex = findNodeIndex(targetId);
            nodes.splice(IpIndex, 1);
            //update();
        }
    }
}

function addConnection (appName, ip, image, serverImage, length, direction) {
    appID = "_" + appName.replace(/\./g, "_");
    ipID = "_" + ip.replace(/\./g, "_");
    if (findNode(appID) === undefined)
        addNode(appID, image);
    if (findNode(ipID) === undefined)
        addServerNode(ipID, serverImage);
    if (direction === "incoming") {
        addLink(ipID, appID, length);
    } else if (direction === "outgoing") {
        addLink(appID, ipID, length);
    }
    //update();
}
