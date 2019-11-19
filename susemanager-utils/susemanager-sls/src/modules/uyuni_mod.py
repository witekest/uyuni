# coding: utf-8
from typing import Any, Dict, List, Optional, Union, Tuple
import ssl
import xmlrpc.client  # type: ignore
import logging


log = logging.getLogger(__name__)

__pillar__: Dict[str, Any] = {}
__context__: Dict[str, Any] = {}
__virtualname__: str = "uyuni"


class UyuniUsersException(Exception):
    """
    Uyuni users Exception
    """


class RPCClient:
    """
    RPC Client
    """
    __instance__: Optional["RPCClient"] = None

    def __init__(self, url: str, user: str, password: str):
        """
        XML-RPC client interface.

        :param url: URL of the remote host
        :param user: username for the XML-RPC endpoints
        :param password: password credentials for the XML-RPC endpoints
        """
        if self.__instance__ is not None:
            raise UyuniUsersException("Object already instantiated. Use init() method instead.")

        ctx: ssl.SSLContext = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE

        self.conn = xmlrpc.client.ServerProxy(url, context=ctx)
        self._user: str = user
        self._password: str = password
        self.token: Optional[str] = None

    @staticmethod
    def init(pillar: Optional[Dict[str, Any]] = None):
        """
        Create new instance

        :return:
        """
        if RPCClient.__instance__ is None:
            plr: Optional[Dict[str, Any]] = __pillar__ or {}
            if "xmlrpc" not in (plr or {}).keys():
                plr = pillar

            if "xmlrpc" in (plr or {}).get("uyuni", {}):
                rpc_conf = (plr or {})["uyuni"]["xmlrpc"] or {}
                RPCClient.__instance__ = RPCClient(rpc_conf.get("url", "https://localhost/rpc/api"),
                                                   rpc_conf.get("user", ""), rpc_conf.get("password", ""))
            else:
                raise UyuniUsersException("Unable to find Pillar configuration for Uyuni XML-RPC API")

        return RPCClient.__instance__

    def get_token(self, refresh: bool = False) -> Optional[str]:
        """
        Authenticate.

        :return:
        """
        if self.token is None or refresh:
            try:
                self.token = self.conn.auth.login(self._user, self._password)
            except Exception as exc:
                log.error("Unable to login to the Uyuni server: %s", exc)

        return self.token

    def __call__(self, method: str, *args, **kwargs):
        self.get_token()
        if self.token is not None:
            try:
                return getattr(self.conn, method)(*args)
            except Exception as exc:
                log.debug("Fall back to the second try due to %s", exc)
                self.get_token(refresh=True)
                try:
                    return getattr(self.conn, method)(*args)
                except Exception as exc:
                    log.error("Unable to call RPC function: %s", exc)
                    raise UyuniUsersException(exc)

        raise UyuniUsersException("XML-RPC backend authentication error.")


class UyuniRemoteObject:
    """
    RPC client
    """
    def __init__(self, pillar: Optional[Dict[str, Any]] = None):
        self.client: RPCClient = RPCClient.init(pillar=pillar)


class UyuniUser(UyuniRemoteObject):
    """
    CRUD operation on users.
    """
    def get_user(self, name: str) -> Dict[str, Any]:
        """
        Get existing user data from the Uyuni.

        :return:
        """
        return self.client("user.getDetails", self.client.get_token(), name)

    def get_all_users(self):
        """
        Return all Uyuni users.

        :return:
        """
        return self.client("user.listUsers", self.client.get_token())

    def create(self, uid: str, password: str, email: str, first_name: str = "", last_name: str = "") -> bool:
        """
        Create user in Uyuni.

        :param uid: desired login name, safe to use if login name is already in use
        :param password: desired password for the user
        :param email: valid email address
        :param first_name: First name
        :param last_name: Second name

        :return: boolean
        """
        log.debug("Adding user to Uyuni")
        if not email:
            ret = 0
            log.debug("Not all parameters has been specified")
            log.error("Email should be specified when create user")
        else:
            ret = self.client("user.create", self.client.get_token(), uid, password, first_name, last_name, email)
            log.debug("User has been created")

        return bool(ret)

    def delete(self, name: str) -> bool:
        """
        Remove user from the Uyuni org.

        :param name: UID of the user

        :return: boolean, True if user has been deleted successfully.
        """
        try:
            ret = bool(self.client("user.delete", self.client.get_token(), name))
        except UyuniUsersException as exc:
            log.error('Unable to delete user "%s": %s', name, exc)
            ret = False
        except Exception as exc:
            log.error('Unhandled error had happend while deleting user "%s": %s', name, exc)
            ret = False

        return ret


class UyuniOrg(UyuniRemoteObject):
    """
    CRUD operations on orgs
    """
    def get_org_by_name(self, name: str) -> Dict[str, Union[int, str, bool]]:
        """
        Get org data by name.

        :param name: organisation name
        :return:
        """
        try:
            org_data = self.client("org.getDetails", self.client.get_token(), name)
        except UyuniUsersException:
            org_data = {}

        return org_data

    def create(self, name: str, admin_login: str, admin_password: str, admin_prefix: str, first_name: str,
               last_name: str, email: str, pam: bool) -> Dict[str, Union[str, int, bool]]:
        """
        Create Uyuni org.

        :param name:
        :param admin_login:
        :param admin_password:
        :param admin_prefix:
        :param first_name:
        :param last_name:
        :param email:
        :param pam:
        :return:
        """
        return self.client("org.create", self.client.get_token(), name, admin_login, admin_password, admin_prefix,
                           first_name, last_name, email, pam)

    def delete(self, name: str) -> Tuple[bool, str]:
        """
        Delete Uyuni org.

        :param name:
        :return: boolean, True if organisation has been deleted.
        """
        org_id = int(self.get_org_by_name(name=name).get("id", -1))
        if org_id > -1:
            res = bool(self.client("org.delete", self.client.get_token(), org_id))
            msg = 'Organisation "{}" (ID: "{}") has been removed.'.format(name, org_id)
            log.debug(msg)
        else:
            res = False
            msg = 'Organisation "{}" was not found.'.format(name)
            log.error(msg)

        return res, msg


def __virtual__():
    """
    Provide Uyuni Users state module.

    :return:
    """

    return __virtualname__
