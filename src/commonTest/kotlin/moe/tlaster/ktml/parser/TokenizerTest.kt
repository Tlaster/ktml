package moe.tlaster.ktml.parser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import moe.tlaster.ktml.Ktml
import moe.tlaster.ktml.parser.token.*

class TokenizerTest {

    @Test
    fun testSimpleHtml() {
        val html = "<html><body><div>Hello</div></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("html"),
            Tag("body"),
            Tag("div"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body"),
            EndTag("html")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testHtmlWithAttributes() {
        val html = "<html><body><div id=\"hello\" class=\"world\">Hello</div></body></html>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("html"),
            Tag("body"),
            Tag("div"),
            Attribute("id", "hello"),
            Attribute("class", "world"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body"),
            EndTag("html")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testWithoutHtmlTag() {
        val html = "<body><div>Hello</div></body>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Tag("body"),
            Tag("div"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testTextFirst() {
        val html = "Hello<body><div>Hello</div></body>"
        val tokens = Ktml.tokenize(html)
        val expectTokens = listOf(
            Text("Hello"),
            Tag("body"),
            Tag("div"),
            Text("Hello"),
            EndTag("div"),
            EndTag("body")
        )
        assertContentEquals(expectTokens, tokens)
    }

    @Test
    fun testGoogle() {
        val html = "<!doctype html><html itemscope=\"\" itemtype=\"http://schema.org/WebPage\" lang=\"zh-HK\"><head><meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"><meta content=\"/images/branding/googleg/1x/googleg_standard_color_128dp.png\" itemprop=\"image\"><title>Google</title><script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){window.google={kEI:'kOH1YqfFNK7u2roP_MOtyAM',kEXPI:'0,1302536,56873,6059,206,4804,2316,383,246,5,5367,1123753,1197789,612,380089,16115,28684,17572,4859,1361,9290,3027,4747,12835,4998,13228,3847,10623,22740,6674,1279,2743,148,1943,6297,109,3405,606,2023,1777,520,14670,603,2625,2844,7,4773,24301,4698,17605,3,576,6460,148,13975,4,1528,2304,27348,4764,2658,7356,11444,2215,4437,16786,5830,2527,4094,4052,3,1167,2374,1,42154,2,14022,14116,11623,5679,1021,2380,28741,4568,6259,23418,1252,5835,14967,4333,12,6077,1395,445,2,2,1,17306,7320,2006,8155,6582,799,1479,13201,2163,5178,14455,7,1922,9779,24,6863,891,9861,4140,1941,702,6899,1749,1624,1460,5954,1980,285,3423,3530,308,552,983,123,700,4,1,2,2,2,2,3718,2235,1122,7,1,768,549,2,1181,139,1644,922,206,181,1242,1206,2323,1248,1080,678,59,2721,1073,612,1234,137,238,278,278,1403,304,14,82,326,329,2,73,220,474,336,114,1324,161,771,511,82,167,197,4,36,334,2,141,1107,163,604,1276,2469,403,654,535,110,4,393,169,4,143,145,98,642,954,1435,8,1084,46,246,5363586,5455,8799655,3311,141,795,19735,1,1,348,1005,149,15,16,1,3,3,1,10,3,6,4,23949267,2738463,1303679,1964,2935,159,1358,7815,2031,2374,3406,717,6456',kBL:'CkVr'};google.sn='webhp';google.kHL='zh-HK';})();(function(){\n" +
            "var f=this||self;var h,k=[];function l(a){for(var b;a&&(!a.getAttribute||!(b=a.getAttribute(\"eid\")));)a=a.parentNode;return b||h}function m(a){for(var b=null;a&&(!a.getAttribute||!(b=a.getAttribute(\"leid\")));)a=a.parentNode;return b}\n" +
            "function n(a,b,c,d,g){var e=\"\";c||-1!==b.search(\"&ei=\")||(e=\"&ei=\"+l(d),-1===b.search(\"&lei=\")&&(d=m(d))&&(e+=\"&lei=\"+d));d=\"\";!c&&f._cshid&&-1===b.search(\"&cshid=\")&&\"slh\"!==a&&(d=\"&cshid=\"+f._cshid);c=c||\"/\"+(g||\"gen_204\")+\"?atyp=i&ct=\"+a+\"&cad=\"+b+e+\"&zx=\"+Date.now()+d;/^http:/i.test(c)&&\"https:\"===window.location.protocol&&(google.ml&&google.ml(Error(\"a\"),!1,{src:c,glmm:1}),c=\"\");return c};h=google.kEI;google.getEI=l;google.getLEI=m;google.ml=function(){return null};google.log=function(a,b,c,d,g){if(c=n(a,b,c,d,g)){a=new Image;var e=k.length;k[e]=a;a.onerror=a.onload=a.onabort=function(){delete k[e]};a.src=c}};google.logUrl=n;}).call(this);(function(){\n" +
            "google.y={};google.sy=[];google.x=function(a,b){if(a)var c=a.id;else{do c=Math.random();while(google.y[c])}google.y[c]=[a,b];return!1};google.sx=function(a){google.sy.push(a)};google.lm=[];google.plm=function(a){google.lm.push.apply(google.lm,a)};google.lq=[];google.load=function(a,b,c){google.lq.push([[a],b,c])};google.loadAll=function(a,b){google.lq.push([a,b])};google.bx=!1;google.lx=function(){};}).call(this);google.f={};(function(){\n" +
            "document.documentElement.addEventListener(\"submit\",function(b){var a;if(a=b.target){var c=a.getAttribute(\"data-submitfalse\");a=\"1\"===c||\"q\"===c&&!a.elements.q.value?!0:!1}else a=!1;a&&(b.preventDefault(),b.stopPropagation())},!0);document.documentElement.addEventListener(\"click\",function(b){var a;a:{for(a=b.target;a&&a!==document.documentElement;a=a.parentElement)if(\"A\"===a.tagName){a=\"1\"===a.getAttribute(\"data-nohref\");break a}a=!1}a&&b.preventDefault()},!0);}).call(this);</script><style>#gbar,#guser{font-size:13px;padding-top:1px !important;}#gbar{height:22px}#guser{padding-bottom:7px !important;text-align:right}.gbh,.gbd{border-top:1px solid #c9d7f1;font-size:1px}.gbh{height:0;position:absolute;top:24px;width:100%}@media all{.gb1{height:22px;margin-right:.5em;vertical-align:top}#gbar{float:left}}a.gb1,a.gb4{text-decoration:underline !important}a.gb1,a.gb4{color:#00c !important}.gbi .gb4{color:#dd8e27 !important}.gbf .gb4{color:#900 !important}\n" +
            "</style><style>body,td,a,p,.h{font-family:arial,sans-serif}body{margin:0;overflow-y:scroll}#gog{padding:3px 8px 0}td{line-height:.8em}.gac_m td{line-height:17px}form{margin-bottom:20px}.h{color:#1558d6}em{color:#c5221f;font-style:normal;font-weight:normal}a em{text-decoration:underline}.lst{height:25px;width:496px}.gsfi,.lst{font:18px arial,sans-serif}.gsfs{font:17px arial,sans-serif}.ds{display:inline-box;display:inline-block;margin:3px 0 4px;margin-left:4px}input{font-family:inherit}body{background:#fff;color:#000}a{color:#4b11a8;text-decoration:none}a:hover,a:active{text-decoration:underline}.fl a{color:#1558d6}a:visited{color:#4b11a8}.sblc{padding-top:5px}.sblc a{display:block;margin:2px 0;margin-left:13px;font-size:11px}.lsbb{background:#f8f9fa;border:solid 1px;border-color:#dadce0 #70757a #70757a #dadce0;height:30px}.lsbb{display:block}#WqQANb a{display:inline-block;margin:0 12px}.lsb{background:url(/images/nav_logo229.png) 0 -261px repeat-x;border:none;color:#000;cursor:pointer;height:30px;margin:0;outline:0;font:15px arial,sans-serif;vertical-align:top}.lsb:active{background:#dadce0}.lst:focus{outline:none}</style><script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){window.google.erd={jsr:1,bv:1632,de:true};\n" +
            "var h=this||self;var k,l=null!=(k=h.mei)?k:1,n,p=null!=(n=h.sdo)?n:!0,q=0,r,t=google.erd,v=t.jsr;google.ml=function(a,b,d,m,e){e=void 0===e?2:e;b&&(r=a&&a.message);if(google.dl)return google.dl(a,e,d),null;if(0>v){window.console&&console.error(a,d);if(-2===v)throw a;b=!1}else b=!a||!a.message||\"Error loading script\"===a.message||q>=l&&!m?!1:!0;if(!b)return null;q++;d=d||{};b=encodeURIComponent;var c=\"/gen_204?atyp=i&ei=\"+b(google.kEI);google.kEXPI&&(c+=\"&jexpid=\"+b(google.kEXPI));c+=\"&srcpg=\"+b(google.sn)+\"&jsr=\"+b(t.jsr)+\"&bver=\"+b(t.bv);var f=a.lineNumber;void 0!==f&&(c+=\"&line=\"+f);var g=\n" +
            "a.fileName;g&&(0<g.indexOf(\"-extension:/\")&&(e=3),c+=\"&script=\"+b(g),f&&g===window.location.href&&(f=document.documentElement.outerHTML.split(\"\\n\")[f],c+=\"&cad=\"+b(f?f.substring(0,300):\"No script found.\")));c+=\"&jsel=\"+e;for(var u in d)c+=\"&\",c+=b(u),c+=\"=\",c+=b(d[u]);c=c+\"&emsg=\"+b(a.name+\": \"+a.message);c=c+\"&jsst=\"+b(a.stack||\"N/A\");12288<=c.length&&(c=c.substr(0,12288));a=c;m||google.log(0,\"\",a);return a};window.onerror=function(a,b,d,m,e){r!==a&&(a=e instanceof Error?e:Error(a),void 0===d||\"lineNumber\"in a||(a.lineNumber=d),void 0===b||\"fileName\"in a||(a.fileName=b),google.ml(a,!1,void 0,!1,\"SyntaxError\"===a.name||\"SyntaxError\"===a.message.substring(0,11)||0<a.message.indexOf(\"Script error\")?2:0));r=null;p&&q>=l&&(window.onerror=null)};})();</script></head><body bgcolor=\"#fff\"><script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){var src='/images/nav_logo229.png';var iesg=false;document.body.onload = function(){window.n && window.n();if (document.images){new Image().src=src;}\n" +
            "if (!iesg){document.f&&document.f.q.focus();document.gbqf&&document.gbqf.q.focus();}\n" +
            "}\n" +
            "})();</script><div id=\"mngb\"><div id=gbar><nobr><b class=gb1>&#25628;&#23563;</b> <a class=gb1 href=\"https://www.google.com.hk/imghp?hl=zh-TW&tab=wi\">&#22294;&#29255;</a> <a class=gb1 href=\"https://maps.google.com.hk/maps?hl=zh-TW&tab=wl\">&#22320;&#22294;</a> <a class=gb1 href=\"https://play.google.com/?hl=zh-TW&tab=w8\">Play</a> <a class=gb1 href=\"https://www.youtube.com/?gl=HK&tab=w1\">YouTube</a> <a class=gb1 href=\"https://news.google.com/?tab=wn\">&#26032;&#32862;</a> <a class=gb1 href=\"https://mail.google.com/mail/?tab=wm\">Gmail</a> <a class=gb1 href=\"https://drive.google.com/?tab=wo\">&#38642;&#31471;&#30828;&#30879;</a> <a class=gb1 style=\"text-decoration:none\" href=\"https://www.google.com.hk/intl/zh-TW/about/products?tab=wh\"><u>&#26356;&#22810;</u> &raquo;</a></nobr></div><div id=guser width=100%><nobr><span id=gbn class=gbi></span><span id=gbf class=gbf></span><span id=gbe></span><a href=\"http://www.google.com.hk/history/optout?hl=zh-TW\" class=gb4>&#32178;&#38913;&#35352;&#37636;</a> | <a  href=\"/preferences?hl=zh-TW\" class=gb4>&#35373;&#23450;</a> | <a target=_top id=gb_70 href=\"https://accounts.google.com/ServiceLogin?hl=zh-TW&passive=true&continue=https://www.google.com/&ec=GAZAAQ\" class=gb4>&#30331;&#20837;</a></nobr></div><div class=gbh style=left:0></div><div class=gbh style=right:0></div></div><center><br clear=\"all\" id=\"lgpd\"><div id=\"lga\"><img alt=\"Google\" height=\"92\" src=\"/images/branding/googlelogo/1x/googlelogo_white_background_color_272x92dp.png\" style=\"padding:28px 0 14px\" width=\"272\" id=\"hplogo\"><br><br></div><form action=\"/search\" name=\"f\"><table cellpadding=\"0\" cellspacing=\"0\"><tr valign=\"top\"><td width=\"25%\">&nbsp;</td><td align=\"center\" nowrap=\"\"><input name=\"ie\" value=\"ISO-8859-1\" type=\"hidden\"><input value=\"zh-HK\" name=\"hl\" type=\"hidden\"><input name=\"source\" type=\"hidden\" value=\"hp\"><input name=\"biw\" type=\"hidden\"><input name=\"bih\" type=\"hidden\"><div class=\"ds\" style=\"height:32px;margin:4px 0\"><input class=\"lst\" style=\"margin:0;padding:5px 8px 0 6px;vertical-align:top;color:#000\" autocomplete=\"off\" value=\"\" title=\"Google &#25628;&#23563;\" maxlength=\"2048\" name=\"q\" size=\"57\"></div><br style=\"line-height:0\"><span class=\"ds\"><span class=\"lsbb\"><input class=\"lsb\" value=\"Google &#25628;&#23563;\" name=\"btnG\" type=\"submit\"></span></span><span class=\"ds\"><span class=\"lsbb\"><input class=\"lsb\" id=\"tsuid1\" value=\"&#22909;&#25163;&#27683;\" name=\"btnI\" type=\"submit\"><script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){var id='tsuid1';document.getElementById(id).onclick = function(){if (this.form.q.value){this.checked = 1;if (this.form.iflsig)this.form.iflsig.disabled = false;}\n" +
            "else top.location='/doodles/';};})();</script><input value=\"AJiK0e8AAAAAYvXvoIj5bXeFrte9-pKkiy4uQwrZftfM\" name=\"iflsig\" type=\"hidden\"></span></span></td><td class=\"fl sblc\" align=\"left\" nowrap=\"\" width=\"25%\"><a href=\"/advanced_search?hl=zh-HK&amp;authuser=0\">&#36914;&#38542;&#25628;&#23563;</a></td></tr></table><input id=\"gbv\" name=\"gbv\" type=\"hidden\" value=\"1\"><script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){\n" +
            "var a,b=\"1\";if(document&&document.getElementById)if(\"undefined\"!=typeof XMLHttpRequest)b=\"2\";else if(\"undefined\"!=typeof ActiveXObject){var c,d,e=[\"MSXML2.XMLHTTP.6.0\",\"MSXML2.XMLHTTP.3.0\",\"MSXML2.XMLHTTP\",\"Microsoft.XMLHTTP\"];for(c=0;d=e[c++];)try{new ActiveXObject(d),b=\"2\"}catch(h){}}a=b;if(\"2\"==a&&-1==location.search.indexOf(\"&gbv=2\")){var f=google.gbvu,g=document.getElementById(\"gbv\");g&&(g.value=a);f&&window.setTimeout(function(){location.href=f},0)};}).call(this);</script></form><div id=\"gac_scont\"></div><div style=\"font-size:83%;min-height:3.5em\"><br><div id=\"gws-output-pages-elements-homepage_additional_languages__als\"><style>#gws-output-pages-elements-homepage_additional_languages__als{font-size:small;margin-bottom:24px}#SIvCob{color:#3c4043;display:inline-block;line-height:28px;}#SIvCob a{padding:0 3px;}.H6sW5{display:inline-block;margin:0 2px;white-space:nowrap}.z4hgWe{display:inline-block;margin:0 2px}</style><div id=\"SIvCob\">Google &#36879;&#36942;&#20197;&#19979;&#35486;&#35328;&#25552;&#20379;&#65306;  <a href=\"https://www.google.com/setprefs?sig=0_tsXEFrYMX7hZivdqxsiirlyIf3Y%3D&amp;hl=zh-CN&amp;source=homepage&amp;sa=X&amp;ved=0ahUKEwinzfLIxcD5AhUut1YBHfxhCzkQ2ZgBCAU\">&#20013;&#25991;(&#31616;&#20307;)</a>    <a href=\"https://www.google.com/setprefs?sig=0_tsXEFrYMX7hZivdqxsiirlyIf3Y%3D&amp;hl=en&amp;source=homepage&amp;sa=X&amp;ved=0ahUKEwinzfLIxcD5AhUut1YBHfxhCzkQ2ZgBCAY\">English</a>  </div></div></div><span id=\"footer\"><div style=\"font-size:10pt\"><div style=\"margin:19px auto;text-align:center\" id=\"WqQANb\"><a href=\"/intl/zh-TW/about.html\">&#38364;&#26044; Google</a><a href=\"https://www.google.com/setprefdomain?prefdom=HK&amp;prev=https://www.google.com.hk/&amp;sig=K_tl1oiqY7XqJ6550L2tl9CVwXgmU%3D\">Google.com.hk</a></div></div><p style=\"font-size:8pt;color:#70757a\">&copy; 2022 - <a href=\"/intl/zh-TW/policies/privacy/\">&#31169;&#38577;&#27402;&#25919;&#31574;</a> - <a href=\"/intl/zh-TW/policies/terms/\">&#26781;&#27454;</a></p></span></center><script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){window.google.cdo={height:757,width:1440};(function(){\n" +
            "var a=window.innerWidth,b=window.innerHeight;if(!a||!b){var c=window.document,d=\"CSS1Compat\"==c.compatMode?c.documentElement:c.body;a=d.clientWidth;b=d.clientHeight}a&&b&&(a!=google.cdo.width||b!=google.cdo.height)&&google.log(\"\",\"\",\"/client_204?&atyp=i&biw=\"+a+\"&bih=\"+b+\"&ei=\"+google.kEI);}).call(this);})();</script> <script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){google.xjs={ck:'xjs.hp.wV_mtd6jOzc.L.X.O',cs:'ACT90oE8p7n4ga-mWyYZacgHnEv7Rvgalg',excm:[]};})();</script>  <script nonce=\"oIxCXC4BA9SSrOPx2vYG4Q\">(function(){var u='/xjs/_/js/k\\x3dxjs.hp.en.k5CZMAtuKq8.O/am\\x3dAMATAIAEACg/d\\x3d1/ed\\x3d1/rs\\x3dACT90oEJzCAGb4tdrN3Jh48o8KXxAFW9Sg/m\\x3dsb_he,d';\n" +
            "var d=this||self,e=function(a){return a};\n" +
            "var g;var l=function(a,b){this.g=b===h?a:\"\"};l.prototype.toString=function(){return this.g+\"\"};var h={};function n(){var a=u;google.lx=function(){p(a);google.lx=function(){}};google.bx||google.lx()}\n" +
            "function p(a){google.timers&&google.timers.load&&google.tick&&google.tick(\"load\",\"xjsls\");var b=document;var c=\"SCRIPT\";\"application/xhtml+xml\"===b.contentType&&(c=c.toLowerCase());c=b.createElement(c);if(void 0===g){b=null;var k=d.trustedTypes;if(k&&k.createPolicy){try{b=k.createPolicy(\"goog#html\",{createHTML:e,createScript:e,createScriptURL:e})}catch(q){d.console&&d.console.error(q.message)}g=b}else g=b}a=(b=g)?b.createScriptURL(a):a;a=new l(a,h);c.src=a instanceof l&&a.constructor===l?a.g:\"type_error:TrustedResourceUrl\";var f,m;(f=(a=null==(m=(f=(c.ownerDocument&&c.ownerDocument.defaultView||window).document).querySelector)?void 0:m.call(f,\"script[nonce]\"))?a.nonce||a.getAttribute(\"nonce\")||\"\":\"\")&&c.setAttribute(\"nonce\",f);document.body.appendChild(c);google.psa=!0};google.xjsu=u;setTimeout(function(){n()},0);})();function _DumpException(e){throw e;}\n" +
            "function _F_installCss(c){}\n" +
            "(function(){google.jl={blt:'none',chnk:0,dw:false,dwu:true,emtn:0,end:0,ine:false,injs:'none',injt:0,injth:0,injv2:false,lls:'default',pdt:0,rep:0,snet:true,strt:0,ubm:false,uwp:true};})();(function(){var pmc='{\\x22d\\x22:{},\\x22sb_he\\x22:{\\x22agen\\x22:true,\\x22cgen\\x22:true,\\x22client\\x22:\\x22heirloom-hp\\x22,\\x22dh\\x22:true,\\x22dhqt\\x22:true,\\x22ds\\x22:\\x22\\x22,\\x22ffql\\x22:\\x22zh-TW\\x22,\\x22fl\\x22:true,\\x22host\\x22:\\x22google.com\\x22,\\x22isbh\\x22:28,\\x22msgs\\x22:{\\x22cibl\\x22:\\x22&#28165;&#38500;&#25628;&#23563;\\x22,\\x22dym\\x22:\\x22&#20320;&#26159;&#19981;&#26159;&#35201;&#26597;&#65306;\\x22,\\x22lcky\\x22:\\x22&#22909;&#25163;&#27683;\\x22,\\x22lml\\x22:\\x22&#30637;&#35299;&#35443;&#24773;\\x22,\\x22oskt\\x22:\\x22&#36664;&#20837;&#24037;&#20855;\\x22,\\x22psrc\\x22:\\x22&#24050;&#24478;&#24744;&#30340;&#12300;\\\\u003Ca href\\x3d\\\\\\x22/history\\\\\\x22\\\\u003E&#32178;&#38913;&#35352;&#37636;\\\\u003C/a\\\\u003E&#12301;&#20013;&#31227;&#38500;&#36889;&#31558;&#25628;&#23563;&#35352;&#37636;\\x22,\\x22psrl\\x22:\\x22&#31227;&#38500;\\x22,\\x22sbit\\x22:\\x22&#20197;&#22294;&#25628;&#23563;\\x22,\\x22srch\\x22:\\x22Google &#25628;&#23563;\\x22},\\x22ovr\\x22:{},\\x22pq\\x22:\\x22\\x22,\\x22refpd\\x22:true,\\x22rfs\\x22:[],\\x22sbas\\x22:\\x220 3px 8px 0 rgba(0,0,0,0.2),0 0 0 1px rgba(0,0,0,0.08)\\x22,\\x22sbpl\\x22:16,\\x22sbpr\\x22:16,\\x22scd\\x22:10,\\x22stok\\x22:\\x22RO2O7Y2qmUF4R4DHmQ0rrUsLLr0\\x22,\\x22uhde\\x22:false}}';google.pmc=JSON.parse(pmc);})();</script>        </body></html>"
        val tokes = Ktml.tokenize(html)
        println(tokes)
    }
}