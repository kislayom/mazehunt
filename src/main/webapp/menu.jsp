<html>
    <head>
        <script language="javascript" type="text/javascript"  src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js"></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/lib/arbor.js" ></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/demos/_/graphics.js" ></script>
        <script language="javascript" type="text/javascript" src="assets/arbor/demos/halfviz/src/renderer.js" ></script>
    </head>
    <body>
    <div class="col-md-12 text-left">
        <span class="glyphicon glyphicon-home" aria-hidden="true"> <a href="gamecontainer.jsp">Home</a></span>
        <span class="glyphicon glyphicon-apple" aria-hidden="true"> <a href="#instruction" onclick="load('instruction');return false;">Instruction Set</a></span>
    </div>

      <canvas id="viewport" width="300" height="800"></canvas>
      <script language="javascript" type="text/javascript">
            var sys = arbor.ParticleSystem(1000, 400,1);
            sys.parameters({gravity:true});
            sys.renderer = Renderer("#viewport") ;
            var data = {
               nodes:{
                 question101:{'color':'red','shape':'dot','label':'question101'},
                 question102:{'color':'green','shape':'dot','label':'question102'},
                 question103:{'color':'blue','shape':'dot','label':'question103'},
                 question104:{'color':'orange','shape':'dot','label':'question104'},
                 question105:{'color':'green','shape':'dot','label':'question105'},
                 question106:{'color':'blue','shape':'dot','label':'question106'},
                 question107:{'color':'blue','shape':'dot','label':'question107'},
               }, 
                edges:{
				 question101:{ question102:{},question103:{}, },
                 question102:{ question104:{}, question105:{} },
                 question103:{ question106:{} ,question107:{} }
               }
             };
            sys.graft(data);
         
      </script>
    </body>
</html>
@RC1140
Owner
